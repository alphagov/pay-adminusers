package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompleteInviteResponse {

    private Invite invite;
    @Schema(example = "287cg75v3737")
    private String userExternalId;
    @Schema(example = "89wi6il2364328")
    private String serviceExternalId;

    public CompleteInviteResponse(Invite invite, String userExternalId) {
        this.invite = invite;
        this.userExternalId = userExternalId;
    }

    public Invite getInvite() {
        return invite;
    }

    public String getUserExternalId() {
        return userExternalId;
    }

    public String getServiceExternalId() {
        return serviceExternalId;
    }

    public void setServiceExternalId(String serviceExternalId) {
        this.serviceExternalId = serviceExternalId;
    }
}
