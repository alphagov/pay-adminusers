package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonGetter;
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

import static java.lang.Boolean.FALSE;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class User {

    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_SERVICE_IDS = "service_ids";
    public static final String FIELD_GATEWAY_ACCOUNT_IDS = "gateway_account_ids";
    public static final String FIELD_TELEPHONE_NUMBER = "telephone_number";
    public static final String FIELD_OTP_KEY = "otp_key";
    public static final String FIELD_ROLE_NAME = "role_name";

    private Integer id;
    private String username;
    private String password;
    private String email;
    private List<String> gatewayAccountIds = new ArrayList<>();
    private String telephoneNumber;
    private List<String> serviceIds = new ArrayList<>();
    private String otpKey;
    private Boolean disabled = FALSE;
    private Integer loginCounter = 0;
    private List<Role> roles = new ArrayList<>();
    private List<Link> links = new ArrayList<>();
    private Integer sessionVersion = 0;

    public static User from(Integer id, String username, String password, String email,
                            List<String> gatewayAccountIds, String otpKey, String telephoneNumber) {
        return new User(id, username, password, email, gatewayAccountIds, otpKey, telephoneNumber);
    }

    public static User from(Integer id, String username, String password, String email,
                            List<String> gatewayAccountIds, List<String> serviceIds, String otpKey, String telephoneNumber) {
        return new User(id, username, password, email, gatewayAccountIds, serviceIds, otpKey, telephoneNumber);
    }

    public static User from(JsonNode node) {
        List<String> serviceIds = new ArrayList<>();
        try {
            List<String> gatewayAccountIds =
                    ImmutableList.copyOf(node.get(FIELD_GATEWAY_ACCOUNT_IDS).iterator())
                            .stream().map(JsonNode::asText)
                            .sorted(Comparators.usingNumericComparator())
                            .collect(Collectors.toList());
            JsonNode serviceIdsNode = node.get(FIELD_SERVICE_IDS);
            if (serviceIdsNode != null) {
               serviceIds =
                        ImmutableList.copyOf(serviceIdsNode.iterator())
                                .stream().map(JsonNode::asText)
                                .sorted(Comparators.usingNumericComparator())
                                .collect(Collectors.toList());
            }
            String username = node.get(FIELD_USERNAME).asText();
            String password = getOrElseRandom(node.get(FIELD_PASSWORD));
            String email = node.get(FIELD_EMAIL).asText();
            String telephoneNumber = node.get(FIELD_TELEPHONE_NUMBER).asText();
            String otpKey = getOrElseRandom(node.get(FIELD_OTP_KEY));
            return from(randomInt(), username, password, email, gatewayAccountIds, serviceIds, otpKey, telephoneNumber);
        } catch (NullPointerException e) {
            throw new RuntimeException("Error retrieving required fields for creating a user", e);
        }
    }

    private static String getOrElseRandom(JsonNode elementNode) {
        return elementNode == null || isBlank(elementNode.asText()) ? newId() : elementNode.asText();
    }

    private User(Integer id, @JsonProperty("username") String username, @JsonProperty("password") String password,
                 @JsonProperty("email") String email, @JsonProperty("gateway_account_ids") List<String> gatewayAccountIds,
                 @JsonProperty("otp_key") String otpKey, @JsonProperty("telephone_number") String telephoneNumber) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.gatewayAccountIds = gatewayAccountIds;
        this.otpKey = otpKey;
        this.telephoneNumber = telephoneNumber;
    }

    private User(Integer id, String username, String password, String email, List<String> gatewayAccountIds,
                 List<String> serviceIds, String otpKey, String telephoneNumber) {
        this.id = id;
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

    public String getOtpKey() {
        return otpKey;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    @JsonGetter
    public Boolean isDisabled() {
        return disabled;
    }

    public Integer getLoginCounter() {
        return loginCounter;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public void setLoginCounter(Integer loginCounter) {
        this.loginCounter = loginCounter;
    }

    public void setSessionVersion(Integer sessionVersion) {
        this.sessionVersion = sessionVersion;
    }

    public Integer getSessionVersion() {
        return sessionVersion;
    }

    @JsonIgnore
    public List<String> getServiceIds() {
        return serviceIds;
    }

    /**
     * We've agreed that given we are currently supporting only 1 role per user we will not json output a list of 1 role
     * instead the flat role attribute below.
     *
     * @return
     */
    @JsonIgnore
    public List<Role> getRoles() {
        return roles;
    }

    /**
     * Only for the json payload as described above
     *
     * @return
     */
    @JsonProperty("role")
    public Role getRole() {
        return roles != null ? roles.get(0) : null;
    }

    @JsonProperty("permissions")
    public List<String> getPermissions() {
        return roles != null ?
                roles.stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(Permission::getName)
                        .collect(Collectors.toList())
                : emptyList();
    }


    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    @JsonIgnore
    public Integer getId() {
        return id;
    }

    @JsonProperty("_links")
    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    /**
     * its probably not a good idea to toString() password / otpKey
     *
     * @return
     */
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", gatewayAccountIds=[" + String.join(", ", gatewayAccountIds) + ']' +
                ", disabled=" + disabled +
                ", roles=" + roles +
                '}';
    }
}
