package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InviteValidateOtpRequest {

    public static final String FIELD_CODE = "code";
    public static final String FIELD_OTP = "otp";

    private final String code;
    private final int otpCode;

    private InviteValidateOtpRequest(String code, int otpCode) {
        this.code = code;
        this.otpCode = otpCode;
    }

    public static InviteValidateOtpRequest from(JsonNode jsonNode) {
        return new InviteValidateOtpRequest(jsonNode.get(FIELD_CODE).asText(), jsonNode.get(FIELD_OTP).asInt());
    }

    @Schema(example = "d02jddeib0lqpsir28fbskg9v0rv", required = true, maxLength = 255)
    public String getCode() {
        return code;
    }

    @Schema(example = "123456", required = true)
    public int getOtpCode() {
        return otpCode;
    }
}
