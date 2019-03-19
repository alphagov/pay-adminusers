package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import io.restassured.http.ContentType;
import org.junit.Test;
import uk.gov.pay.adminusers.fixtures.InviteDbFixture;

import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.*;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;

public class InviteResourceGenerateOtpTest extends IntegrationTest {

    private String code;

    private static final String OTP_KEY = newId();
    private static final String EMAIL = "invited-" + random(5) + "@example.com";
    private static final String TELEPHONE_NUMBER = "+447999999999";
    private static final String PASSWORD = "a-secure-password";


    @Test
    public void generateOtp_shouldSucceed_forUserInvite_evenWhenTokenIsExpired_sinceItShouldBeValidatedOnGetInvite() throws Exception {
        givenAnExistingUserInvite();
        ImmutableMap<Object, Object> invitationRequest = ImmutableMap.builder()
                .put("telephone_number", TELEPHONE_NUMBER)
                .put("password", PASSWORD)
                .build();

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
        ImmutableMap<Object, Object> invitationRequest = ImmutableMap.builder()
                .put("telephone_number", TELEPHONE_NUMBER)
                .put("password", PASSWORD)
                .build();

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
        ImmutableMap<Object, Object> invitationRequest = ImmutableMap.builder()
                .build();

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
