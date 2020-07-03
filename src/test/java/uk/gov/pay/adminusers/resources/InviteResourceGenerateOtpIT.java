package uk.gov.pay.adminusers.resources;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.fixtures.InviteDbFixture;

import java.util.Map;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;

public class InviteResourceGenerateOtpIT extends IntegrationTest {

    private String code;

    private static final String OTP_KEY = newId();
    private static final String EMAIL = "invited-" + random(5) + "@example.com";
    private static final String TELEPHONE_NUMBER = "+447999999999";
    private static final String PASSWORD = "a-secure-password";


    @Test
    public void generateOtp_shouldSucceed_forUserInvite_evenWhenTokenIsExpired_sinceItShouldBeValidatedOnGetInvite() throws Exception {
        givenAnExistingUserInvite();
        Map<Object, Object> invitationRequest = Map.of(
                "telephone_number", TELEPHONE_NUMBER,
                "password", PASSWORD);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(format(INVITES_GENERATE_OTP_RESOURCE_URL, code))
                .then()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void generateOtp_should_FailforUserInvite_whenInviteDoesNotExist() throws Exception {
        Map<Object, Object> invitationRequest = Map.of(
                "telephone_number", TELEPHONE_NUMBER,
                "password", PASSWORD);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(format(INVITES_GENERATE_OTP_RESOURCE_URL, "not-existing-code"))
                .then()
                .statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    public void generateOtp_shouldFail_forUserInvite_whenAllMandatoryFieldsAreMissing() throws Exception {
        givenAnExistingUserInvite();
        Map<Object, Object> invitationRequest = emptyMap();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(format(INVITES_GENERATE_OTP_RESOURCE_URL, code))
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());
    }


    @Test
    public void generateOtp_shouldSucceed_forServiceInvite_evenWhenTokenIsExpired_sinceItShouldBeValidatedOnGetInvite() {
        givenAnExistingServiceInvite();
        givenSetup()
                .when()
                .contentType(ContentType.JSON)
                .post(format(INVITES_GENERATE_OTP_RESOURCE_URL, code))
                .then()
                .statusCode(OK.getStatusCode());
    }

    private void givenAnExistingUserInvite() {
        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withEmail(EMAIL)
                .withOtpKey(OTP_KEY)
                .expired()
                .insertInvite();
    }

    private void givenAnExistingServiceInvite() {
        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withEmail(EMAIL)
                .withOtpKey(OTP_KEY)
                .insertServiceInvite();
    }
}
