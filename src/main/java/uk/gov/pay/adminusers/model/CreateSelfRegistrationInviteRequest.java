package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateSelfRegistrationInviteRequest {

    @NotEmpty
    @Email
    protected String email;
    protected String telephoneNumber;
    protected String password;

    public CreateSelfRegistrationInviteRequest() {
        // for Jackson
    }

    public CreateSelfRegistrationInviteRequest(String email) {
        this.email = email;
    }

    public CreateSelfRegistrationInviteRequest(String email, String telephoneNumber, String password) {
        this.email = email;
        this.telephoneNumber = telephoneNumber;
        this.password = password;
    }

    @Schema(example = "example@example.gov.uk", required = true)
    public String getEmail() {
        return email;
    }

    @Schema(example = "070xxxxxxxx")
    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    @Schema(example = "password123!")
    public String getPassword() {
        return password;
    }
}
