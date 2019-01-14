package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.User;

import static com.google.common.io.BaseEncoding.base32;
import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class UserResourceSecondFactorAuthenticationTest extends IntegrationTest {

    private static final String USER_2FA_AUTHENTICATE_URL = USER_2FA_URL + "/authenticate";
    private static final String OTP_KEY = "34f34";

    private String externalId;
    private String username;

    @Before
    public void createValidUser() {
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withOtpKey(OTP_KEY).withUsername(username).withEmail(email).insertUser();

        this.externalId = user.getExternalId();
        this.username = user.getUsername();
    }


    @Test
    public void shouldCreate2FA_forAValidNewSecondFactorPasscodeRequest_withNoBody() {
        givenSetup()
                .when()
                .accept(JSON)
                .post(format(USER_2FA_URL, externalId))
                .then()
                .statusCode(200);
    }

    @Test
    public void shouldCreate2FA_forAValidNewSecondFactorPasscodeRequest_withProvisionalFalse() throws JsonProcessingException {
        ImmutableMap<String, Boolean> body = ImmutableMap.of("provisional", false);

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(body))
                .post(format(USER_2FA_URL, externalId))
                .then()
                .statusCode(200);
    }

    @Test
    public void shouldCreate2FA_forAValidNewSecondFactorPasscodeRequest_withProvisionalTrue() throws JsonProcessingException {
        databaseHelper.updateProvisionalOtpKey(username, "ABCDEFGHIJKLMNOP");

        ImmutableMap<String, Boolean> body = ImmutableMap.of("provisional", true);

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(body))
                .post(format(USER_2FA_URL, externalId))
                .then()
                .statusCode(200);
    }

    @Test
    public void shouldReturnNotFound_forAValidNewSecondFactorPasscodeRequest_withProvisionalTrue_ifNoProvisionalOtpKey() throws JsonProcessingException {
        ImmutableMap<String, Boolean> body = ImmutableMap.of("provisional", true);

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(body))
                .post(format(USER_2FA_URL, externalId))
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldAuthenticate2FA_forAValid2FAAuthRequest() throws Exception {
        GoogleAuthenticator testAuthenticator = new GoogleAuthenticator();
        int passcode = testAuthenticator.getTotpPassword(base32().encode(OTP_KEY.getBytes()));
        ImmutableMap<String, Integer> authBody = ImmutableMap.of("code", passcode);

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(authBody))
                .post(format(USER_2FA_AUTHENTICATE_URL, externalId))
                .then()
                .statusCode(200)
                .body("username", is(username))
                .body("login_counter", is(0))
                .body("disabled", is(false));
    }

    @Test
    public void shouldReturnNotFound_forNonExistentUser_when2FAAuthCreateRequest() {
        String nonExistingExternalId = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        givenSetup()
                .when()
                .accept(JSON)
                .post(format(USER_2FA_URL, nonExistingExternalId))
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldReturnUnauthorized_onInvalid2FACode_during2FAAuth() throws Exception {
        int invalidPasscode = 111111;
        ImmutableMap<String, Integer> authBody = ImmutableMap.of("code", invalidPasscode);

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(authBody))
                .post(format(USER_2FA_AUTHENTICATE_URL, externalId))
                .then()
                .statusCode(401);
    }

    @Test
    public void shouldReturnUnauthorizedAndAccountLocked_during2FAAuth_ifMaxRetryExceeded() throws Exception {
        databaseHelper.updateLoginCount(username, 10);

        int invalidPasscode = 111111;
        ImmutableMap<String, Integer> authBody = ImmutableMap.of("code", invalidPasscode);

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(authBody))
                .post(format(USER_2FA_AUTHENTICATE_URL, externalId))
                .then()
                .statusCode(401);

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .get(format(USER_RESOURCE_URL, externalId))
                .then()
                .statusCode(200)
                .body("username", is(username))
                .body("login_counter", is(11))
                .body("disabled", is(true));
    }
}
