package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.collect.ImmutableList;
import uk.gov.pay.adminusers.utils.Comparators;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

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

    private final String username;
    private final String password;
    private final String email;
    private final List<String> gatewayAccountIds;
    private final List<String> serviceExternalIds;
    private final String telephoneNumber;
    private final String otpKey;
    private final String features;

    public static CreateUserRequest from(String username, String password, String email,
                                         List<String> gatewayAccountIds, List<String> serviceExternalIds, String otpKey, String telephoneNumber, String features) {
        return new CreateUserRequest(username, password, email, gatewayAccountIds, serviceExternalIds, otpKey, telephoneNumber, features);
    }

    public static CreateUserRequest from(JsonNode node) {
        List<String> gatewayAccountIds = new ArrayList<>();
        List<String> serviceExternalIds = new ArrayList<>();
        try {
            if (node.get(FIELD_GATEWAY_ACCOUNT_IDS) != null) {
                gatewayAccountIds =
                        ImmutableList.copyOf(node.get(FIELD_GATEWAY_ACCOUNT_IDS).iterator())
                                .stream().map(JsonNode::asText)
                                .sorted(Comparators.numericallyThenLexicographically())
                                .collect(Collectors.toList());
            }
            if (node.get(FIELD_SERVICE_EXTERNAL_IDS) != null) {
                serviceExternalIds =
                        ImmutableList.copyOf(node.get(FIELD_SERVICE_EXTERNAL_IDS).iterator())
                                .stream().map(JsonNode::asText)
                                .collect(Collectors.toList());
            }
            String username = node.get(FIELD_USERNAME).asText();
            String password = getOrElseRandom(node.get(FIELD_PASSWORD), randomUuid());
            String email = node.get(FIELD_EMAIL).asText();
            String telephoneNumber = node.get(FIELD_TELEPHONE_NUMBER).asText();
            String otpKey = getOrElseRandom(node.get(FIELD_OTP_KEY), newId());
            String features = getOrElseRandom(node.get(FIELD_FEATURES), null);
            return from(username, password, email, gatewayAccountIds, serviceExternalIds, otpKey, telephoneNumber, features);
        } catch (NullPointerException e) {
            throw new RuntimeException("Error retrieving required fields for creating a user", e);
        }
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
