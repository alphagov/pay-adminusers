package uk.gov.pay.adminusers.resources;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.fixtures.InviteDbFixture;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.GONE;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import static jakarta.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

class InviteResourceOtpIT extends IntegrationTest {

    private static final String OTP_KEY = "KPWXGUTNWOE7PMVK";
    private static final int PASSCODE = new GoogleAuthenticator().getTotpPassword(OTP_KEY);
    private static final String EMAIL = "invited-" + random(5) + "@example.com";
    private static final String TELEPHONE_NUMBER = "+447999999999";
    private static final String PASSWORD = "a-secure-password";
    
    private Role adminRole;
    private String code;

    @BeforeEach
    void givenAnExistingInvite() {
        adminRole = getInjector().getInstance(RoleDao.class).findByRoleName(RoleName.ADMIN).get().toRole();
        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withEmail(EMAIL)
                .withOtpKey(OTP_KEY)
                .expired()
                .insertInviteToAddUserToService(adminRole);
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
                .insertInviteToAddUserToService(adminRole);

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
    void validateOtp_shouldFailAndLockInvite_whenInvalidOtpAuthCode_ifMaxRetryExceeded() throws Exception {

        // create an invitation
        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withEmail(EMAIL)
                .withOtpKey(OTP_KEY)
                .withTelephoneNumber(TELEPHONE_NUMBER)
                .withPassword(PASSWORD)
                .withLoginCounter(9)
                .insertInviteToAddUserToService(adminRole);

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
                .statusCode(422)
                .body("errors", hasSize(2))
                .body("errors", hasItems("code must not be empty", "otp must not be empty"));
    }

    @Test
    void validateOtp_shouldFail_whenOtpIsNotNumeric() throws Exception {
        Map<Object, Object> sendRequest = Map.of(
                "code", code,
                "otp", "not-numeric");
        
        givenSetup()
                .when()
                .body(mapper.writeValueAsString(sendRequest))
                .contentType(JSON)
                .post(INVITES_VALIDATE_OTP_RESOURCE_URL)
                .then()
                .statusCode(422)
                .body("errors", hasSize(1))
                .body("errors", hasItems("otp must be numeric"));
    }


    @Test
    void validateOtp_shouldSucceed_whenValidOtp() throws Exception {

        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withOtpKey(OTP_KEY)
                .insertInviteToAddUserToService(adminRole);

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
                .insertInviteToAddUserToService(adminRole);

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
    void sendOtp_shouldSendNotification_whenInviteHasTelephoneNumber() {
        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withTelephoneNumber("01234567890")
                .withOtpKey(OTP_KEY)
                .insertInviteToAddUserToService(adminRole);

        givenSetup()
                .when()
                .contentType(JSON)
                .post(format(INVITES_SEND_OTP_RESOURCE_URL, code))
                .then()
                .statusCode(NO_CONTENT.getStatusCode());
    }

    @Test
    void sendOtp_shouldFail_whenInviteNotFound() {
        givenSetup()
                .when()
                .contentType(JSON)
                .post(format(INVITES_SEND_OTP_RESOURCE_URL, "non-existent-invite"))
                .then()
                .statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    void sendOtp_shouldFail_whenInviteCodeMalformatted() {
        givenSetup()
                .when()
                .contentType(JSON)
                .post(format(INVITES_SEND_OTP_RESOURCE_URL, ""))
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    void sendOtp_shouldFail_whenInviteHasNoTelephoneNumber() {
        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withTelephoneNumber(null)
                .withOtpKey(OTP_KEY)
                .insertInviteToAddUserToService(adminRole);
        
        givenSetup()
                .when()
                .contentType(JSON)
                .post(format(INVITES_SEND_OTP_RESOURCE_URL, code))
                .then()
                .statusCode(PRECONDITION_FAILED.getStatusCode());
    }
}
