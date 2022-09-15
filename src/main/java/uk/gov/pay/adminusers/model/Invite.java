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
    private InviteType type;
    private boolean userExist = false;
    private boolean expired;
    private boolean passwordSet;

    public Invite(String code, String email, String telephoneNumber,
                  Boolean disabled, Integer attemptCounter, String type, String role, Boolean expired, boolean passwordSet) {
        this.code = code;
        this.email = email;
        this.telephoneNumber = telephoneNumber;
        this.disabled = disabled;
        this.attemptCounter = attemptCounter;
        this.type = InviteType.from(type);
        this.role = role;
        this.expired = expired;
        this.passwordSet = passwordSet;
    }

    @JsonProperty("email")
    @Schema(example = "example@example.gov.uk")
    public String getEmail() {
        return email;
    }

    @JsonProperty("telephone_number")
    @Schema(example = "+440787654534")
    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    @JsonProperty("disabled")
    @Schema(example = "false")
    public Boolean isDisabled() {
        return disabled;
    }

    @JsonProperty("attempt_counter")
    @Schema(example = "0")
    public Integer getAttemptCounter() {
        return attemptCounter;
    }

    @JsonProperty("_links")
    public List<Link> getLinks() {
        return links;
    }

    @JsonProperty("role")
    @Schema(example = "view-only")
    public String getRole() {
        return role;
    }

    @JsonProperty("expired")
    @Schema(example = "false")
    public Boolean isExpired() {
        return expired;
    }

    @JsonProperty("password_set")
    @Schema(example = "false")
    public boolean isPasswordSet() {
        return passwordSet;
    }

    public void setInviteLink(String targetUrl) {
        Link inviteLink = Link.from(Link.Rel.INVITE, "GET", targetUrl);
        this.links.add(inviteLink);
    }

    @Schema(example = "service")
    public InviteType getType() {
        return type;
    }

    public void setType(String type) {
        this.type = InviteType.from(type);
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
