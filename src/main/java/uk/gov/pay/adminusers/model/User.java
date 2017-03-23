package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

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
    private String externalId;
    private String username;
    private String password;
    private String email;
    private List<String> gatewayAccountIds = new ArrayList<>();
    private String telephoneNumber;
    private List<String> serviceIds = new ArrayList<>();
    private String otpKey;
    private Boolean disabled = Boolean.FALSE;
    private Integer loginCounter = 0;
    private List<Role> roles = new ArrayList<>();
    private List<Link> links = new ArrayList<>();
    private Integer sessionVersion = 0;
    private String serviceName;

    public static User from(Integer id, String externalId, String username, String password, String email,
                            List<String> gatewayAccountIds, List<String> serviceIds, String otpKey, String telephoneNumber) {
        return new User(id, externalId, username, password, email, gatewayAccountIds, serviceIds, otpKey, telephoneNumber);
    }

    private User(Integer id, @JsonProperty("external_id") String externalId, @JsonProperty("username") String username, @JsonProperty("password") String password,
                 @JsonProperty("email") String email, @JsonProperty("gateway_account_ids") List<String> gatewayAccountIds,
                 List<String> serviceIds, @JsonProperty("otp_key") String otpKey, @JsonProperty("telephone_number") String telephoneNumber) {
        this.id = id;
        this.externalId = externalId;
        this.username = username;
        this.password = password;
        this.email = email;
        this.gatewayAccountIds = gatewayAccountIds;
        this.serviceIds = serviceIds;
        this.otpKey = otpKey;
        this.telephoneNumber = telephoneNumber;
    }

    @JsonIgnore
    public Integer getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
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
                "externalId=" + externalId +
                ", gatewayAccountIds=[" + String.join(", ", gatewayAccountIds) + ']' +
                ", disabled=" + disabled +
                ", roles=" + roles +
                '}';
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
