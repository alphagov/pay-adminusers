package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

public class InviteOtpRequest {

    public static final String FIELD_CODE = "code";
    public static final String FIELD_TELEPHONE_NUMBER = "telephone_number";
    public static final String FIELD_PASSWORD = "password";

    @Deprecated //until "/otp/resend" is refactored into "{code}/otp/resend"
    private String code;
    private String telephoneNumber;
    private String password;

    private InviteOtpRequest() {
    }

    private InviteOtpRequest(String code, String telephoneNumber, String password) {
        this.code = code;
        this.telephoneNumber = telephoneNumber;
        this.password = password;
    }

    public static InviteOtpRequest from(JsonNode jsonNode) {
        if(jsonNode == null || !jsonNode.fieldNames().hasNext()) {
            return new InviteOtpRequest();
        } else {
            String password = Optional.ofNullable(jsonNode.get(FIELD_PASSWORD)).map(JsonNode::asText).orElse(null);
            String code = Optional.ofNullable(jsonNode.get(FIELD_CODE)).map(JsonNode::asText).orElse(null);
            return new InviteOtpRequest(code, jsonNode.get(FIELD_TELEPHONE_NUMBER).asText(), password);
        }
    }

    @Deprecated
    public String getCode() {
        return code;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public String getPassword() {
        return password;
    }
}
