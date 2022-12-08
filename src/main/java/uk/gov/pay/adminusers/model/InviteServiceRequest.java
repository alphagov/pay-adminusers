package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.adminusers.validations.ValidTelephoneNumber;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InviteServiceRequest {

    @NotEmpty
    @Email
    protected String email;
    
    public InviteServiceRequest() {
        // for Jackson
    }
    
    public InviteServiceRequest(String email) {
        this.email = email;
    }

    @Schema(example = "example@example.gov.uk", required = true)
    public String getEmail() {
        return email;
    }

}
