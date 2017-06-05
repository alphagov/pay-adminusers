package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class InviteRequest {

    public static final String FIELD_SENDER = "sender";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_ROLE_NAME = "role_name";
    public static final String FIELD_OTP_KEY = "otp_key";

    private final String sender;
    private final String email;
    private final String roleName;
    private final String otpKey;

    private InviteRequest(String sender, String email, String roleName, String otpKey) {
        this.sender = sender;
        this.email = email;
        this.roleName = roleName;
        this.otpKey = otpKey;
    }

    public static InviteRequest from(JsonNode jsonNode) {
        return new InviteRequest(jsonNode.get(FIELD_SENDER).asText(), jsonNode.get(FIELD_EMAIL).asText(), jsonNode.get(FIELD_ROLE_NAME).asText(), getOrElseRandom(jsonNode.get(FIELD_OTP_KEY)));
    }

    public String getSender() {
        return sender;
    }

    public String getEmail() {
        return email;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getOtpKey() {
        return otpKey;
    }

    private static String getOrElseRandom(JsonNode elementNode) {
        return elementNode == null || isBlank(elementNode.asText()) ? randomUuid() : elementNode.asText();
    }
}
