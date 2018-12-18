package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;

public class InviteUserRequest extends InviteRequest {

    public static final String FIELD_SENDER = "sender";
    public static final String FIELD_SERVICE_EXTERNAL_ID = "service_external_id";

    private final String sender;

    private final String serviceExternalId;

    private InviteUserRequest(String sender, String email, String roleName, String otpKey, String serviceExternalId) {
        super(roleName, email, otpKey);
        this.sender = sender;
        this.serviceExternalId = serviceExternalId;
    }

    public static InviteUserRequest from(JsonNode jsonNode) {
        return new InviteUserRequest(
                jsonNode.get(FIELD_SENDER).asText(),
                jsonNode.get(FIELD_EMAIL).asText(),
                jsonNode.get(FIELD_ROLE_NAME).asText(),
                getOrElseRandom(jsonNode.get(FIELD_OTP_KEY)),
                jsonNode.get(FIELD_SERVICE_EXTERNAL_ID).asText()
        );
    }

    @Deprecated
    public static InviteUserRequest from(JsonNode jsonNode, String serviceExternalId) {
        return new InviteUserRequest(jsonNode.get(FIELD_SENDER).asText(),
                jsonNode.get(FIELD_EMAIL).asText(),
                jsonNode.get(FIELD_ROLE_NAME).asText(),
                getOrElseRandom(jsonNode.get(FIELD_OTP_KEY)),
                serviceExternalId
        );
    }

    public String getSender() {
        return sender;
    }

    public String getServiceExternalId() {
        return serviceExternalId;
    }

}
