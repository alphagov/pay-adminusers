package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class InviteServiceRequest extends InviteRequest {
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_TELEPHONE_NUMBER = "telephone_number";
    private static final String DEFAULT_ROLE_NAME = "admin";

    private String password;
    private String telephoneNumber;

    private InviteServiceRequest(String roleName,
                                 String password,
                                 String email,
                                 String telephoneNumber) {
        super(roleName, email);
        this.password = password;
        this.telephoneNumber = telephoneNumber;
    }

    public InviteServiceRequest(
            String password,
            String email,
            String telephoneNumber) {
        this(DEFAULT_ROLE_NAME, password, email, telephoneNumber);
    }

    public InviteServiceRequest(String email) {
        this(DEFAULT_ROLE_NAME, null, email, null);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public void setTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }


    public static InviteServiceRequest from(JsonNode payload) {
        return new InviteServiceRequest(
                DEFAULT_ROLE_NAME,
                Optional.ofNullable(payload.get(FIELD_PASSWORD)).map(JsonNode::asText).orElse(null),
                payload.get(FIELD_EMAIL).asText(),
                Optional.ofNullable(payload.get(FIELD_TELEPHONE_NUMBER)).map(JsonNode::asText).orElse(null)
        );
    }

}
