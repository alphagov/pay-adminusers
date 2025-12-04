package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateSelfRegistrationInviteRequest {

    @NotEmpty
    @Email
    protected String email;
    
    public CreateSelfRegistrationInviteRequest() {
        // for Jackson
    }
    
    public CreateSelfRegistrationInviteRequest(String email) {
        this.email = email;
    }

    @Schema(example = "example@example.gov.uk", requiredMode = REQUIRED)
    public String getEmail() {
        return email;
    }

}
