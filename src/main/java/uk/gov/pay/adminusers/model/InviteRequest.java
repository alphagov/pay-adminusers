package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;

public class InviteRequest {

    public static final String FIELD_SENDER = "sender";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_ROLE_NAME = "role_name";

    private final String sender;
    private final String email;
    private final String roleName;

    private InviteRequest(String sender, String email, String roleName) {
        this.sender = sender;
        this.email = email;
        this.roleName = roleName;
    }

    public static InviteRequest from(JsonNode jsonNode) {
        return new InviteRequest(jsonNode.get(FIELD_SENDER).asText(), jsonNode.get(FIELD_EMAIL).asText(), jsonNode.get(FIELD_ROLE_NAME).asText());
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
}
