package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(Include.NON_NULL)
public class Invite {

    private String code;
    private final String email;
    private final String role;
    private String telephoneNumber;
    private Boolean disabled = Boolean.FALSE;
    private Integer attemptCounter = 0;

    private List<Link> links = new ArrayList<>();
    private boolean isInviteToJoinService;
    private String type;
    private boolean userExist = false;
    private boolean expired;
    private boolean passwordSet;

    private String otpKey;

    public Invite(String code,
                  String email,
                  String telephoneNumber,
                  Boolean disabled,
                  Integer attemptCounter,
                  boolean isInviteToJoinService,
                  String type,
                  String role,
                  Boolean expired,
                  boolean passwordSet,
                  String otpKey) {
        this.code = code;
        this.email = email;
        this.telephoneNumber = telephoneNumber;
        this.disabled = disabled;
        this.attemptCounter = attemptCounter;
        this.isInviteToJoinService = isInviteToJoinService;
        this.type = type;
        this.role = role;
        this.expired = expired;
        this.passwordSet = passwordSet;
        this.otpKey = otpKey;
    }

    @Schema(example = "example@example.gov.uk")
    public String getEmail() {
        return email;
    }

    @Schema(example = "+440787654534")
    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    @Schema(example = "false")
    public Boolean isDisabled() {
        return disabled;
    }
    
    @Schema(example = "0")
    public Integer getAttemptCounter() {
        return attemptCounter;
    }

    @JsonProperty("_links")
    public List<Link> getLinks() {
        return links;
    }

    @Schema(example = "view-only")
    public String getRole() {
        return role;
    }

    @Schema(example = "false")
    public Boolean isExpired() {
        return expired;
    }

    @Schema(example = "false")
    public boolean isPasswordSet() {
        return passwordSet;
    }

    @Schema(example = "ABC123")
    public String getOtpKey() {
        return otpKey;
    }

    public void setInviteLink(String targetUrl) {
        Link inviteLink = Link.from(Link.Rel.INVITE, "GET", targetUrl);
        this.links.add(inviteLink);
    }
    
    @Schema(example = "true")
    public boolean isInviteToJoinService() {
        return isInviteToJoinService;
    }

    @Schema(example = "service")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonIgnore
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Derived attribute only to indicate if a user with the specified email already exits in the system.
     * This is not stored in database rather populated at runtime every time at the usage.
     *
     * @param userExist
     */
    public void setUserExist(boolean userExist) {
        this.userExist = userExist;
    }

    @JsonProperty("user_exist")
    public boolean isUserExist() {
        return userExist;
    }
}
