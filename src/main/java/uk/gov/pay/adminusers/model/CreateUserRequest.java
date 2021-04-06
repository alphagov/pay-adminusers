package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.utils.Comparators.numericallyThenLexicographically;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class CreateUserRequest {

    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_GATEWAY_ACCOUNT_IDS = "gateway_account_ids";
    public static final String FIELD_SERVICE_EXTERNAL_IDS = "service_external_ids";
    public static final String FIELD_TELEPHONE_NUMBER = "telephone_number";
    public static final String FIELD_OTP_KEY = "otp_key";
    public static final String FIELD_ROLE_NAME = "role_name";
    public static final String FIELD_FEATURES = "features";

    private String username;
    private String password;
    private String email;
    private List<String> gatewayAccountIds;
    private List<String> serviceExternalIds;
    private String telephoneNumber;
    private String otpKey;
    private String features;

    public static CreateUserRequest from(String username, String password, String email,
                                         List<String> gatewayAccountIds, List<String> serviceExternalIds, String otpKey, String telephoneNumber, String features) {
        return new CreateUserRequest(username, password, email, gatewayAccountIds, serviceExternalIds, otpKey, telephoneNumber, features);
    }

    public static CreateUserRequest from(JsonNode node) {
        final List<String> gatewayAccountIds = safelyGetList(node, FIELD_GATEWAY_ACCOUNT_IDS);
        gatewayAccountIds.sort(numericallyThenLexicographically());

        final List<String> serviceExternalIds = safelyGetList(node, FIELD_SERVICE_EXTERNAL_IDS);

        String username = getNodeAsTextOrFail(node, FIELD_USERNAME);
        String password = getOrElseRandom(node.get(FIELD_PASSWORD), randomUuid());
        String email = getNodeAsTextOrFail(node, FIELD_EMAIL);
        String telephoneNumber = getNodeAsTextOrFail(node, FIELD_TELEPHONE_NUMBER);
        String otpKey = getOrElseRandom(node.get(FIELD_OTP_KEY), newId());
        String features = getOrElseRandom(node.get(FIELD_FEATURES), null);
        return from(username, password, email, gatewayAccountIds, serviceExternalIds, otpKey, telephoneNumber, features);
    }

    private static List<String> safelyGetList(JsonNode node, String fieldName) {
        if (node == null || node.get(fieldName) == null) {
            return Collections.emptyList();
        }

        var results = new ArrayList<String>();
        node.get(fieldName)
                .iterator()
                .forEachRemaining(nodeValue -> Optional.ofNullable(nodeValue).map(JsonNode::asText).ifPresent(results::add));
        return results;
    }

    private static String getNodeAsTextOrFail(JsonNode node, String fieldName) {
        return Optional.ofNullable(node.get(fieldName))
                .map(JsonNode::asText)
                .orElseThrow(() -> new RuntimeException(format("Error retrieving field %s for creating a user", fieldName)));
    }

    private static String getOrElseRandom(JsonNode elementNode, String randomValue) {
        return elementNode == null || isBlank(elementNode.asText()) ? randomValue : elementNode.asText();
    }

    private CreateUserRequest(@JsonProperty("username") String username, @JsonProperty("password") String password,
                              @JsonProperty("email") String email,
                              @JsonProperty("gateway_account_ids") List<String> gatewayAccountIds, @JsonProperty("service_external_ids") List<String> serviceExternalIds,
                              @JsonProperty("otp_key") String otpKey, @JsonProperty("telephone_number") String telephoneNumber, @JsonProperty("features") String features) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.gatewayAccountIds = gatewayAccountIds;
        this.serviceExternalIds = serviceExternalIds;
        this.otpKey = otpKey;
        this.telephoneNumber = telephoneNumber;
        this.features = features;
    }

    public String getUsername() {
        return username;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getGatewayAccountIds() {
        return gatewayAccountIds;
    }

    public String getOtpKey() {
        return otpKey;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public String getFeatures() {
        return features;
    }

    public List<String> getServiceExternalIds() {
        return serviceExternalIds;
    }

}
