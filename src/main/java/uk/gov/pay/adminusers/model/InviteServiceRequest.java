package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.adminusers.validations.ValidEmail;
import uk.gov.pay.adminusers.validations.ValidTelephoneNumber;

import javax.validation.constraints.NotEmpty;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InviteServiceRequest {
    
    private String password;

    @NotEmpty
    @ValidEmail
    protected String email;

    @ValidTelephoneNumber
    private String telephoneNumber;
    
    public InviteServiceRequest() {
        // for Jackson
    }
    
    public InviteServiceRequest(
            String password,
            String email,
            String telephoneNumber) {
        this.password = password;
        this.email = email;
        this.telephoneNumber = telephoneNumber;
    }

    public InviteServiceRequest(String email) {
        this(null, email, null);
    }

    @Schema(example = "a-password")
    public String getPassword() {
        return password;
    }

    @Schema(example = "example@example.gov.uk", required = true)
    public String getEmail() {
        return email;
    }

    @Schema(example = "+440787654534")
    public String getTelephoneNumber() {
        return telephoneNumber;
    }

}
