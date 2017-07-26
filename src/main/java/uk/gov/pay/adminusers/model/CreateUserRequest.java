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
    public static final String FIELD_SERVICE_IDS = "service_ids";
    public static final String FIELD_SERVICE_EXTERNAL_IDS = "service_external_ids";
    public static final String FIELD_GATEWAY_ACCOUNT_IDS = "gateway_account_ids";
    public static final String FIELD_TELEPHONE_NUMBER = "telephone_number";
    public static final String FIELD_OTP_KEY = "otp_key";
    public static final String FIELD_ROLE_NAME = "role_name";

    private String username;
    private String password;
    private String email;
    private List<String> gatewayAccountIds = new ArrayList<>();
    private String telephoneNumber;
    @Deprecated //user service external Ids instead
    private List<String> serviceIds = new ArrayList<>();
    private String otpKey;
    private List<String> serviceExternalIds = new ArrayList<>();

    public static CreateUserRequest from(String username, String password, String email,
                                         List<String> gatewayAccountIds, List<String> serviceIds, String otpKey, String telephoneNumber) {
        return new CreateUserRequest(username, password, email, gatewayAccountIds, serviceIds, otpKey, telephoneNumber);
    }

    public static CreateUserRequest from(JsonNode node) {
        List<String>  serviceIds = new ArrayList<>();
        List<String> gatewayAccountIds = new ArrayList<>();
        try {
            if (node.get(FIELD_GATEWAY_ACCOUNT_IDS) != null) {
                gatewayAccountIds =
                        ImmutableList.copyOf(node.get(FIELD_GATEWAY_ACCOUNT_IDS).iterator())
                                .stream().map(JsonNode::asText)
                                .sorted(Comparators.usingNumericComparator())
                                .collect(Collectors.toList());
            }
            //Deprecated .. to remove when full y adopted to external Ids
            JsonNode serviceIdsNode = node.get(FIELD_SERVICE_IDS);
            if (serviceIdsNode != null) {
                serviceIds =
                        ImmutableList.copyOf(serviceIdsNode.iterator())
                                .stream().map(JsonNode::asText)
                                .sorted(Comparators.usingNumericComparator())
                                .collect(Collectors.toList());
            }
            String username = node.get(FIELD_USERNAME).asText();
            String password = getOrElseRandom(node.get(FIELD_PASSWORD), randomUuid());
            String email = node.get(FIELD_EMAIL).asText();
            String telephoneNumber = node.get(FIELD_TELEPHONE_NUMBER).asText();
            String otpKey = getOrElseRandom(node.get(FIELD_OTP_KEY), newId());
            CreateUserRequest request = from(username, password, email, gatewayAccountIds, serviceIds, otpKey, telephoneNumber);
            JsonNode serviceExternalIdsNode = node.get(FIELD_SERVICE_EXTERNAL_IDS);
            if (serviceExternalIdsNode != null) {
                List<String> serviceExternalIds = ImmutableList.copyOf(serviceExternalIdsNode.iterator()).stream().map(jsonNode -> jsonNode.asText()).collect(Collectors.toList());
                request.serviceExternalIds = serviceExternalIds;
            }
            return request;
        } catch (NullPointerException e) {
            throw new RuntimeException("Error retrieving required fields for creating a user", e);
        }
    }

    private static String getOrElseRandom(JsonNode elementNode, String randomValue) {
        return elementNode == null || isBlank(elementNode.asText()) ? randomValue : elementNode.asText();
    }

    private CreateUserRequest(@JsonProperty("username") String username, @JsonProperty("password") String password,
                              @JsonProperty("email") String email, @JsonProperty("gateway_account_ids") List<String> gatewayAccountIds,
                              List<String> serviceIds, @JsonProperty("otp_key") String otpKey, @JsonProperty("telephone_number") String telephoneNumber) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.gatewayAccountIds = gatewayAccountIds;
        this.serviceIds = serviceIds;
        this.otpKey = otpKey;
        this.telephoneNumber = telephoneNumber;
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

    @Deprecated //user service external ids instead
    public List<String> getServiceIds() {
        return serviceIds;
    }

    public String getOtpKey() {
        return otpKey;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public List<String> getServiceExternalIds() {
        return serviceExternalIds;
    }

    public void setServiceExternalIds(List<String> serviceExternalIds) {
        this.serviceExternalIds = serviceExternalIds;
    }
}
