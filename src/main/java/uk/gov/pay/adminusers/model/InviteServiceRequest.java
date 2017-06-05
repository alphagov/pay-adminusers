package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;

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
                                 String telephoneNumber,
                                 String otpKey) {
        super(roleName, email, otpKey);
        this.password = password;
        this.telephoneNumber = telephoneNumber;
    }

    public InviteServiceRequest(
            String password,
            String email,
            String telephoneNumber) {
        this(DEFAULT_ROLE_NAME, password, email, telephoneNumber, randomUuid());
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
                payload.get(FIELD_PASSWORD).asText(),
                payload.get(FIELD_EMAIL).asText(),
                payload.get(FIELD_TELEPHONE_NUMBER).asText(),
                getOrElseRandom(payload.get(FIELD_OTP_KEY))
        );
    }

}
