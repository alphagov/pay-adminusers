package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public abstract class InviteRequest {

    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_ROLE_NAME = "role_name";
    public static final String FIELD_OTP_KEY = "otp_key";

    protected final String roleName;
    protected final String email;
    protected final String otpKey;

    public InviteRequest(String roleName, String email, String otpKey) {
        this.roleName = roleName;
        this.email = email;
        this.otpKey = otpKey;
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

    protected static String getOrElseRandom(JsonNode elementNode) {
        return elementNode == null || isBlank(elementNode.asText()) ? randomUuid() : elementNode.asText();
    }
}
