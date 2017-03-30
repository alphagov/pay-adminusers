package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;

public class Invite {

    private String email;
    private final String roleName;

    private Invite(String email, String roleName) {
        this.email = email;
        this.roleName = roleName;
    }

    public static Invite from(JsonNode json) {
        String email = json.get("email").asText();
        String roleName = json.get("role_name").asText();
        return new Invite(email, roleName);
    }

    public String getEmail() {
        return email;
    }

    public String getRoleName() {
        return roleName;
    }
}
