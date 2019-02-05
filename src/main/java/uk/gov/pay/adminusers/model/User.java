package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class User {

    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_SERVICE_EXTERNAL_ID = "service_external_id";
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_TELEPHONE_NUMBER = "telephone_number";
    public static final String FIELD_ROLE_NAME = "role_name";

    private Integer id;
    private String externalId;
    private String username;
    private String password;
    private String email;
    private String telephoneNumber;
    private String otpKey;
    private Boolean disabled = Boolean.FALSE;
    private Integer loginCounter = 0;
    private String features;
    private List<ServiceRole> serviceRoles = new ArrayList<>();
    private SecondFactorMethod secondFactor;
    private String provisionalOtpKey;
    private ZonedDateTime provisionalOtpKeyCreatedAt;
    private ZonedDateTime lastLoggedInAt;
    private List<Link> links = new ArrayList<>();
    private Integer sessionVersion = 0;

    public static User from(Integer id, String externalId, String username, String password, String email, String otpKey, 
                            String telephoneNumber, List<ServiceRole> serviceRoles, String features,
                            SecondFactorMethod secondFactor, String provisionalOtpKey,
                            ZonedDateTime provisionalOtpKeyCreatedAt, ZonedDateTime lastLoggedInAt) {
        return new User(id, externalId, username, password, email, otpKey, telephoneNumber, serviceRoles, features,
                secondFactor, provisionalOtpKey, provisionalOtpKeyCreatedAt, lastLoggedInAt);
    }

    private User(Integer id, @JsonProperty("external_id") String externalId, @JsonProperty("username") String username,
                 @JsonProperty("password") String password, @JsonProperty("email") String email,
                 @JsonProperty("otp_key") String otpKey, @JsonProperty("telephone_number") String telephoneNumber,
                 @JsonProperty("service_roles") List<ServiceRole> serviceRoles, @JsonProperty("features") String features,
                 @JsonProperty("second_factor") SecondFactorMethod secondFactor,
                 @JsonProperty("provisional_otp_key") String provisionalOtpKey,
                 @JsonProperty("provisional_otp_key_created_at") ZonedDateTime provisionalOtpKeyCreatedAt,
                 @JsonProperty("last_logged_in_at") ZonedDateTime lastLoggedInAt) {
        this.id = id;
        this.externalId = externalId;
        this.username = username;
        this.password = password;
        this.email = email;
        this.otpKey = otpKey;
        this.telephoneNumber = telephoneNumber;
        this.serviceRoles = serviceRoles;
        this.features = features;
        this.secondFactor = secondFactor;
        this.provisionalOtpKey = provisionalOtpKey;
        this.provisionalOtpKeyCreatedAt = provisionalOtpKeyCreatedAt;
        this.lastLoggedInAt = lastLoggedInAt;
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

    public String getFeatures() {
        return features;
    }

    public void setFeatures(String features) {
        this.features = features;
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

    public SecondFactorMethod getSecondFactor() {
        return secondFactor;
    }

    public void setSecondFactor(SecondFactorMethod secondFactor) {
        this.secondFactor = secondFactor;
    }

    public String getProvisionalOtpKey() {
        return provisionalOtpKey;
    }

    public void setProvisionalOtpKey(String provisionalOtpKey) {
        this.provisionalOtpKey = provisionalOtpKey;
    }

    public ZonedDateTime getProvisionalOtpKeyCreatedAt() {
        return provisionalOtpKeyCreatedAt;
    }

    public void setProvisionalOtpKeyCreatedAt(ZonedDateTime provisionalOtpKeyCreatedAt) {
        this.provisionalOtpKeyCreatedAt = provisionalOtpKeyCreatedAt;
    }

    public ZonedDateTime getLastLoggedInAt() {
        return lastLoggedInAt;
    }

    public void setLastLoggedInAt(ZonedDateTime lastLoggedInAt) {
        this.lastLoggedInAt = lastLoggedInAt;
    }

    @JsonProperty("_links")
    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    /**
     * it’s definitely not a good idea to toString() password / otpKey
     *
     * @return
     */
    @Override
    public String toString() {
        return "User{" +
                "externalId=" + externalId +
                ", secondFactor=" + secondFactor +
                ", disabled=" + disabled +
                ", serviceRoles=" + serviceRoles +
                '}';
    }

    public List<ServiceRole> getServiceRoles() {
        return serviceRoles;
    }
}
