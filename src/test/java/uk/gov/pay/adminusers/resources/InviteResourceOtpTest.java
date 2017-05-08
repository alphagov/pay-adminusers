package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.http.ContentType;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.fixtures.InviteDbFixture;

import java.util.List;
import java.util.Map;

import static com.google.common.io.BaseEncoding.base32;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;

public class InviteResourceOtpTest extends IntegrationTest {

    private String code;
    private static final String OTP_KEY = newId();
    private static final String EMAIL = "invited-" + random(5) + "@example.com";

    @Before
    public void givenAnExistingInvite() {
        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withEmail(EMAIL)
                .withOtpKey(OTP_KEY)
                .expired()
                .insertInvite();
    }

    @Test
    public void generateOtp_shouldSucceed_evenWhenTokenIsExpired_sinceItShouldBeValidatedOnGetInvite() throws Exception {

        String telephoneNumber = "+447999999999";

        ImmutableMap<Object, Object> invitationRequest = ImmutableMap.builder()
                .put("code", code)
                .put("telephone_number", telephoneNumber)
                .put("password", "a-secure-password")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(INVITES_GENERATE_OTP_RESOURCE_URL)
                .then()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void generateOtp_shouldFail_whenInviteDoesNotExist() throws Exception {

        String telephoneNumber = "+447999999999";

        ImmutableMap<Object, Object> invitationRequest = ImmutableMap.builder()
                .put("code", "not-existing-code")
                .put("telephone_number", telephoneNumber)
                .put("password", "a-secure-password")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(INVITES_GENERATE_OTP_RESOURCE_URL)
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void createUserUponOtpValidation_shouldCreateUserWhenValidOtp() throws Exception {

        String password = "AsuperEncriptedPassword";
        String telephoneNumber = "+44123456543";

        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withEmail(EMAIL)
                .withOtpKey(OTP_KEY)
                .withTelephoneNumber(telephoneNumber)
                .withPassword(password)
                .expired()
                .insertInvite();

        GoogleAuthenticator testAuthenticator = new GoogleAuthenticator();
        int passcode = testAuthenticator.getTotpPassword(base32().encode(OTP_KEY.getBytes()));

        ImmutableMap<Object, Object> invitationOtpRequest = ImmutableMap.builder()
                .put("code", code)
                .put("otp", passcode)
                .build();

        assertThat(databaseHelper.findInviteByCode(code).size(), is(1));

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationOtpRequest))
                .contentType(ContentType.JSON)
                .post(INVITES_VALIDATE_OTP_RESOURCE_URL)
                .then()
                .statusCode(OK.getStatusCode());

        List<Map<String, Object>> users = databaseHelper.findUserByUsername(EMAIL);

        assertThat(users.size(), is(1));

        Map<String, Object> createdUser = users.get(0);
        assertThat(createdUser.get("username"), is(EMAIL));
        assertThat(createdUser.get("email"), is(EMAIL));
        assertThat(createdUser.get("password"), is(password));
        assertThat(createdUser.get("disabled"), is(false));
    }

    @Test
    public void createUserUponOtpValidation_shouldFail_whenInvalidCode() throws Exception {

        GoogleAuthenticator testAuthenticator = new GoogleAuthenticator();
        int passcode = testAuthenticator.getTotpPassword(base32().encode(OTP_KEY.getBytes()));

        ImmutableMap<Object, Object> invitationOtpRequest = ImmutableMap.builder()
                .put("code", "non-existent-code")
                .put("otp", passcode)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationOtpRequest))
                .contentType(ContentType.JSON)
                .post(INVITES_VALIDATE_OTP_RESOURCE_URL)
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());

    }

    @Test
    public void createUserUponOtpValidation_shouldFail_whenInvalidOtp() throws Exception {

        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withEmail(EMAIL)
                .withOtpKey(OTP_KEY)
                .withTelephoneNumber("+44123456543")
                .withPassword("AsuperEncriptedPassword")
                .expired()
                .insertInvite();

        ImmutableMap<Object, Object> invitationOtpRequest = ImmutableMap.builder()
                .put("code", code)
                .put("otp", 123456)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationOtpRequest))
                .contentType(ContentType.JSON)
                .post(INVITES_VALIDATE_OTP_RESOURCE_URL)
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());
    }
}
