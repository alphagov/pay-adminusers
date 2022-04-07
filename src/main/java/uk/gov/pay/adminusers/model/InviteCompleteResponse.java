package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InviteCompleteResponse {

    private Invite invite;
    @Schema(example = "287cg75v3737")
    private String userExternalId;
    @Schema(example = "89wi6il2364328")
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
