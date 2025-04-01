package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InviteValidateOtpRequest {
    
    @Length(max = 255)
    @NotEmpty
    private String code;
    
    @NotEmpty
    @Pattern(regexp="\\d+", message = "must be numeric")
    private String otp;

    @Schema(example = "d02jddeib0lqpsir28fbskg9v0rv", required = true, maxLength = 255)
    public String getCode() {
        return code;
    }

    @Schema(example = "123456", required = true)
    public int getOtp() {
        return Integer.parseInt(otp);
    }
}
