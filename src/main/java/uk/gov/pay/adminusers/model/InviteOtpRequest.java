package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;

public class InviteOtpRequest {

    public static final String FIELD_CODE = "code";
    public static final String FIELD_TELEPHONE_NUMBER = "telephone_number";
    public static final String FIELD_PASSWORD = "password";

    private final String code;
    private final String telephoneNumber;
    private final String password;

    private InviteOtpRequest(String code, String telephoneNumber, String password) {
        this.code = code;
        this.telephoneNumber = telephoneNumber;
        this.password = password;
    }

    public static InviteOtpRequest from(JsonNode jsonNode) {
        return new InviteOtpRequest(jsonNode.get(FIELD_CODE).asText(), jsonNode.get(FIELD_TELEPHONE_NUMBER).asText(), jsonNode.get(FIELD_PASSWORD).asText());
    }

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
