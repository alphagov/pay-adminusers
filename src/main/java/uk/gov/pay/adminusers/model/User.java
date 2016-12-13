package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

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

    public static final String FIELD_GATEWAY_ACCOUNT_ID = "gateway_account_id";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_OTP_KEY = "otp_key";
    public static final String FIELD_TELEPHONE_NUMBER = "telephone_number";
    public static final String FIELD_ROLE_NAME = "role_name";

    private Integer id;
    private String username;
    private String password;
    private String email;
    private String gatewayAccountId;
    private String otpKey;
    private String telephoneNumber;
    private Boolean disabled = FALSE;
    private Integer loginCount = 0;
    private List<Role> roles = new ArrayList<>();
    private List<Link> links = new ArrayList<>();

    public static User from(String username, String password, String email, String gatewayAccountId, String otpKey, String telephoneNumber) {
        return from(randomInt(), username, password, email, gatewayAccountId, otpKey, telephoneNumber);
    }

    public static User from(Integer id, String username, String password, String email, String gatewayAccountId, String otpKey, String telephoneNumber) {
        return new User(id, username, password, email, gatewayAccountId, otpKey, telephoneNumber);
    }

    public static User from(JsonNode node) {
        try {
            String username = node.get(FIELD_USERNAME).asText();
            String password = getOrElseRandom(node.get(FIELD_PASSWORD));
            String email = node.get(FIELD_EMAIL).asText();
            String telephoneNumber = node.get(FIELD_TELEPHONE_NUMBER).asText();
            String gatewayAccountId = node.get(FIELD_GATEWAY_ACCOUNT_ID).asText();
            String otpKey = getOrElseRandom(node.get(FIELD_OTP_KEY));
            return from(randomInt(), username, password, email, gatewayAccountId, otpKey, telephoneNumber);
        } catch (NullPointerException e) {
            throw new RuntimeException("Error retrieving required fields for creating a user", e);
        }
    }

    private static String getOrElseRandom(JsonNode elementNode) {
        return elementNode == null || isBlank(elementNode.asText()) ? newId() : elementNode.asText();
    }

    private User(Integer id, @JsonProperty("username") String username, @JsonProperty("password") String password,
                 @JsonProperty("email") String email, @JsonProperty("gateway_account_id") String gatewayAccountId,
                 @JsonProperty("otp_key") String otpKey, @JsonProperty("telephone_number") String telephoneNumber) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.gatewayAccountId = gatewayAccountId;
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

    public String getGatewayAccountId() {
        return gatewayAccountId;
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

    public Integer getLoginCount() {
        return loginCount;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public void setLoginCount(Integer loginCount) {
        this.loginCount = loginCount;
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
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", gatewayAccountId='" + gatewayAccountId + '\'' +
                ", disabled=" + disabled +
                ", roles=" + roles +
                '}';
    }
}
