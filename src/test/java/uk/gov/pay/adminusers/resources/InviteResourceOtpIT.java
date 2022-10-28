package uk.gov.pay.adminusers.resources;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.fixtures.InviteDbFixture;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.util.Collections.emptyMap;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

class InviteResourceOtpIT extends IntegrationTest {

    private String code;

    private static final String OTP_KEY = "KPWXGUTNWOE7PMVK";
    private static final int PASSCODE = new GoogleAuthenticator().getTotpPassword(OTP_KEY);
    private static final String EMAIL = "invited-" + random(5) + "@example.com";
    private static final String TELEPHONE_NUMBER = "+447999999999";
    private static final String PASSWORD = "a-secure-password";

    @BeforeEach
    void givenAnExistingInvite() {
        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withEmail(EMAIL)
                .withOtpKey(OTP_KEY)
                .expired()
                .insertInvite();
    }

    @Test
    void validateOtp_shouldFail_whenInvalidCode() throws Exception {

        Map<Object, Object> invitationOtpRequest = Map.of(
                "code", "non-existent-code",
                "otp", PASSCODE);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationOtpRequest))
                .contentType(JSON)
                .post(INVITES_VALIDATE_OTP_RESOURCE_URL)
                .then()
                .statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    void validateOtp_shouldFail_whenInvalidOtpAuthCode() throws Exception {

        // create an invitation
        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withEmail(EMAIL)
                .withOtpKey(OTP_KEY)
                .withTelephoneNumber(TELEPHONE_NUMBER)
                .withPassword(PASSWORD)
                .insertInvite();

        // generate invalid invitationOtpRequest and execute it
        Map<Object, Object> invitationOtpRequest = Map.of(
                "code", code,
                "otp", 123456);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationOtpRequest))
                .contentType(JSON)
                .post(INVITES_VALIDATE_OTP_RESOURCE_URL)
                .then()
                .statusCode(UNAUTHORIZED.getStatusCode());
    }

    @Test
    public void validateOtp_shouldFailAndLockInvite_whenInvalidOtpAuthCode_ifMaxRetryExceeded() throws Exception {

        // create an invitation
        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withEmail(EMAIL)
                .withOtpKey(OTP_KEY)
                .withTelephoneNumber(TELEPHONE_NUMBER)
                .withPassword(PASSWORD)
                .withLoginCounter(9)
                .insertInvite();

        // generate invalid invitationOtpRequest and execute it
        Map<Object, Object> invitationOtpRequest = Map.of(
                "code", code,
                "otp", 123456);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationOtpRequest))
                .contentType(JSON)
                .post(INVITES_VALIDATE_OTP_RESOURCE_URL)
                .then()
                .statusCode(GONE.getStatusCode());

        // check if "login_counter" and "disabled" columns are properly updated
        Map<String, Object> foundInvite = databaseHelper.findInviteByCode(code).get();
        assertThat(foundInvite.get("disabled"), is(Boolean.TRUE));
        assertThat(foundInvite.get("login_counter"), is(10));
    }

    @Test
    void validateOtp_shouldFail_whenAllMandatoryFieldsAreMissing() throws Exception {
        givenSetup()
                .when()
                .body(mapper.writeValueAsString(emptyMap()))
                .contentType(JSON)
                .post(INVITES_VALIDATE_OTP_RESOURCE_URL)
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());
    }



    @Test
    void validateOtp_shouldSucceed_whenValidOtp() throws Exception {

        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withOtpKey(OTP_KEY)
                .insertInvite();

        Map<Object, Object> sendRequest = Map.of(
                "code", code,
                "otp", PASSCODE);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(sendRequest))
                .contentType(JSON)
                .post(INVITES_VALIDATE_OTP_RESOURCE_URL)
                .then()
                .statusCode(NO_CONTENT.getStatusCode());
    }

    @Test
    void validateOtp_shouldFailWith401_whenInvalidOtp() throws Exception {

        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withOtpKey(OTP_KEY)
                .insertInvite();

        int invalidOtp = 111111;

        Map<Object, Object> sendRequest = Map.of(
                "code", code,
                "otp", invalidOtp);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(sendRequest))
                .contentType(JSON)
                .post(INVITES_VALIDATE_OTP_RESOURCE_URL)
                .then()
                .statusCode(UNAUTHORIZED.getStatusCode());
    }

    @Test
    void resendOtp_shouldUpdateTelephoneNumber_whenValidOtp() throws Exception {

        // create an invitation with initial telephone number
        String initialTelephoneNumber = "+447451111111";
        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withEmail(EMAIL)
                .withOtpKey(OTP_KEY)
                .withTelephoneNumber(initialTelephoneNumber)
                .withPassword(PASSWORD)
                .expired()
                .insertInvite();

        // generate new invitationOtpRequest with new telephone number
        String newTelephoneNumber = "+447452222222";
        Map<Object, Object> resendRequest = Map.of(
                "code", code,
                "telephone_number", newTelephoneNumber);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(resendRequest))
                .contentType(JSON)
                .post(INVITES_RESEND_OTP_RESOURCE_URL)
                .then()
                .statusCode(NO_CONTENT.getStatusCode());

        // check if we are using the newTelephoneNumber in the invitation
        Map<String, Object> foundInvite = databaseHelper.findInviteByCode(code).get();
        assertThat(foundInvite.get("telephone_number"), is(newTelephoneNumber));
    }

    @Test
    void resendOtp_shouldFail_whenAllMandatoryFieldsAreMissing() throws Exception {

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(emptyMap()))
                .contentType(JSON)
                .post(INVITES_RESEND_OTP_RESOURCE_URL)
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());
    }
}
