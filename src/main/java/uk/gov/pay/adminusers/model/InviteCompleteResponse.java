package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class InviteCompleteResponse {

    private final Invite invite;
    private String userExternalId;
    private String serviceExternalId;

    public InviteCompleteResponse(@JsonProperty("invite") Invite invite) {
        this.invite = invite;
    }

    public Invite getInvite() {
        return invite;
    }

    public String getUserExternalId() {
        return userExternalId;
    }

    public void setUserExternalId(String userExternalId) {
        this.userExternalId = userExternalId;
    }

    public String getServiceExternalId() {
        return serviceExternalId;
    }

    public void setServiceExternalId(String serviceExternalId) {
        this.serviceExternalId = serviceExternalId;
    }
}
