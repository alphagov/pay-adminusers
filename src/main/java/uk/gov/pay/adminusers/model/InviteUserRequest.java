package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class InviteUserRequest extends InviteRequest {

    public static final String FIELD_SENDER = "sender";

    private final String sender;

    private InviteUserRequest(String sender, String email, String roleName, String otpKey) {
        super(roleName, email, otpKey);
        this.sender = sender;
    }

    public static InviteUserRequest from(JsonNode jsonNode) {
        return new InviteUserRequest(jsonNode.get(FIELD_SENDER).asText(), jsonNode.get(FIELD_EMAIL).asText(), jsonNode.get(FIELD_ROLE_NAME).asText(), getOrElseRandom(jsonNode.get(FIELD_OTP_KEY)));
    }

    public String getSender() {
        return sender;
    }

}
