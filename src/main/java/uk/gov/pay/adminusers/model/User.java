package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newLongId;

public class User {

    @JsonIgnore
    private Long id;
    private String username;
    private String password;
    private String email;
    private String gatewayAccountId;
    private String otpKey;
    private String telephoneNumber;
    private Boolean disabled = FALSE;
    private Integer loginCount = 0;
    private List<Role> roles = new ArrayList<>();

    public static User from(String username, String password, String email, String gatewayAccountId, String otpKey, String telephoneNumber) {
        return from(newLongId(), username, password, email, gatewayAccountId, otpKey, telephoneNumber);
    }

    public static User from(Long id, String username, String password, String email, String gatewayAccountId, String otpKey, String telephoneNumber) {
        return new User(id, username, password, email, gatewayAccountId, otpKey, telephoneNumber);
    }

    private User(Long id, @JsonProperty("username") String username, @JsonProperty("password") String password,
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

    public Boolean getDisabled() {
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

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }
}
