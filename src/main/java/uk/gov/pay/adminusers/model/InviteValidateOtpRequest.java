package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;

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

    public String getCode() {
        return code;
    }

    public int getOtpCode() {
        return otpCode;
    }
}
