package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateInviteToJoinServiceRequest {

    @NotEmpty
    private String sender;

    @NotEmpty
    @Email
    protected String email;

    @NotNull
    private RoleName roleName;

    @NotEmpty
    private String serviceExternalId;
    
    public CreateInviteToJoinServiceRequest() {
        // for Jackson
    }

    public CreateInviteToJoinServiceRequest(String sender, String email, RoleName roleName, String serviceExternalId) {
        this.sender = sender;
        this.email = email;
        this.roleName = roleName;
        this.serviceExternalId = serviceExternalId;
    }

    @Schema(example = "d0wksn12nklsdf1nd02nd9n2ndk", description = "User external ID", required = true)
    public String getSender() {
        return sender;
    }

    @Schema(example = "example@example.gov.uk", required = true)
    public String getEmail() {
        return email;
    }
    
    @Schema(example = "view-only", required = true)
    public RoleName getRoleName() {
        return roleName;
    }
    
    @Schema(example = "dj2jkejke32jfhh3", required = true)
    public String getServiceExternalId() {
        return serviceExternalId;
    }

}
