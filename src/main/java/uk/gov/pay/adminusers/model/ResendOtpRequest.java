package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import org.hibernate.validator.constraints.Length;
import uk.gov.pay.adminusers.validations.ValidTelephoneNumber;

import javax.validation.constraints.NotEmpty;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ResendOtpRequest {

    @NotEmpty
    @Length(max = 255)
    private String code;
    
    @NotEmpty
    @ValidTelephoneNumber
    private String telephoneNumber;

    public ResendOtpRequest() {
        // for Jackson
    }

    public ResendOtpRequest(String code, String telephoneNumber) {
        this.code = code;
        this.telephoneNumber = telephoneNumber;
    }

    @Schema(example = "d02jddeib0lqpsir28fbskg9v0rv", maxLength = 255, required = true)
    public String getCode() {
        return code;
    }

    @Schema(example = "+440787654534", required = true)
    public String getTelephoneNumber() {
        return telephoneNumber;
    }
}
