package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.ArrayList;
import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(Include.NON_NULL)
public class Invite {

    private String code;
    private final String email;
    private final String role;
    private String telephoneNumber;
    private Boolean disabled = Boolean.FALSE;
    private Integer attemptCounter = 0;

    private List<Link> links = new ArrayList<>();
    private String type;
    private boolean userExist = false;
    private boolean expired;

    public Invite(String code, String email, String telephoneNumber,
                  Boolean disabled, Integer attemptCounter, String type, String role, Boolean expired) {
        this.code = code;
        this.email = email;
        this.telephoneNumber = telephoneNumber;
        this.disabled = disabled;
        this.attemptCounter = attemptCounter;
        this.type = type;
        this.role = role;
        this.expired = expired;
    }

    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    @JsonProperty("telephone_number")
    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    @JsonProperty("disabled")
    public Boolean isDisabled() {
        return disabled;
    }

    @JsonProperty("attempt_counter")
    public Integer getAttemptCounter() {
        return attemptCounter;
    }

    @JsonProperty("_links")
    public List<Link> getLinks() {
        return links;
    }

    @JsonProperty("role")
    public String getRole() {
        return role;
    }
    
    @JsonProperty("expired")
    public Boolean isExpired() {
        return expired;
    }
    
    public void setInviteLink(String targetUrl) {
        Link inviteLink = Link.from(Link.Rel.invite, "GET", targetUrl);
        this.links.add(inviteLink);
    }

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
