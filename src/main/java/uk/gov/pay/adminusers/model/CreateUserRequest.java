package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.utils.Comparators.numericallyThenLexicographically;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateUserRequest {

    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_GATEWAY_ACCOUNT_IDS = "gateway_account_ids";
    public static final String FIELD_SERVICE_EXTERNAL_IDS = "service_external_ids";
    public static final String FIELD_TELEPHONE_NUMBER = "telephone_number";
    public static final String FIELD_OTP_KEY = "otp_key";
    public static final String FIELD_ROLE_NAME = "role_name";
    public static final String FIELD_FEATURES = "features";

    private String password;
    @Schema(example = "user@somegovernmentdept.gov.uk")
    private String email;
    @ArraySchema(schema = @Schema(example = "1"))
    private List<String> gatewayAccountIds;
    @ArraySchema(schema = @Schema(example = "7d19aff33f8948deb97ed16b2912dcd3"))
    private List<String> serviceExternalIds;
    @Schema(example = "447700900000")
    private String telephoneNumber;
    @Schema(example = "43c3c4t")
    private String otpKey;
    @Schema(example = "feature1, feature2")
    private String features;

    public static CreateUserRequest from(String password, String email,
                                         List<String> gatewayAccountIds, List<String> serviceExternalIds, String otpKey, String telephoneNumber, String features) {
        return new CreateUserRequest(password, email, gatewayAccountIds, serviceExternalIds, otpKey, telephoneNumber, features);
    }

    public static CreateUserRequest from(JsonNode node) {
        final List<String> gatewayAccountIds = safelyGetList(node, FIELD_GATEWAY_ACCOUNT_IDS);
        gatewayAccountIds.sort(numericallyThenLexicographically());

        final List<String> serviceExternalIds = safelyGetList(node, FIELD_SERVICE_EXTERNAL_IDS);

           String password = getOrElseRandom(node.get(FIELD_PASSWORD), randomUuid());
        String email = getNodeAsTextOrFail(node, FIELD_EMAIL);
        String telephoneNumber = getNodeAsTextOrFail(node, FIELD_TELEPHONE_NUMBER);
        String otpKey = getNodeAsTextNullable(node, FIELD_OTP_KEY);
        String features = getOrElseRandom(node.get(FIELD_FEATURES), null);
        return from(password, email, gatewayAccountIds, serviceExternalIds, otpKey, telephoneNumber, features);
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
    
    private static String getNodeAsTextNullable(JsonNode node, String fieldName) {
        return Optional.ofNullable(node.get(fieldName))
                .map(JsonNode::asText)
                .orElse(null);
    }

    private static String getOrElseRandom(JsonNode elementNode, String randomValue) {
        return elementNode == null || isBlank(elementNode.asText()) ? randomValue : elementNode.asText();
    }

    private CreateUserRequest(@JsonProperty("password") String password,
                              @JsonProperty("email") String email,
                              @JsonProperty("gateway_account_ids") List<String> gatewayAccountIds, @JsonProperty("service_external_ids") List<String> serviceExternalIds,
                              @JsonProperty("otp_key") String otpKey, @JsonProperty("telephone_number") String telephoneNumber, @JsonProperty("features") String features) {
        this.password = password;
        this.email = email;
        this.gatewayAccountIds = gatewayAccountIds;
        this.serviceExternalIds = serviceExternalIds;
        this.otpKey = otpKey;
        this.telephoneNumber = telephoneNumber;
        this.features = features;
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

    public Optional<String> getOtpKey() {
        return Optional.ofNullable(otpKey);
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
