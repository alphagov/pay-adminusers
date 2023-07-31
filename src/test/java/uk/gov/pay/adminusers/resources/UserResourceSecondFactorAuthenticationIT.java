package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.User;

import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

class UserResourceSecondFactorAuthenticationIT extends IntegrationTest {

    private static final String USER_2FA_AUTHENTICATE_URL = USER_2FA_URL + "/authenticate";
    private static final String OTP_KEY = "KPWXGUTNWOE7PMVK";

    private String externalId;
    private String username;

    @BeforeEach
    void createValidUser() {
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withOtpKey(OTP_KEY).withEmail(email).insertUser();

        this.externalId = user.getExternalId();
        this.username = user.getUsername();
    }


    @Test
    void shouldCreate2FA_forAValidNewSecondFactorPasscodeRequest_withNoBody() {
        givenSetup()
                .when()
                .accept(JSON)
                .post(format(USER_2FA_URL, externalId))
                .then()
                .statusCode(200);
    }

    @Test
    void shouldCreate2FA_forAValidNewSecondFactorPasscodeRequest_withProvisionalFalse() throws JsonProcessingException {
        Map<String, Boolean> body = Map.of("provisional", false);

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(body))
                .post(format(USER_2FA_URL, externalId))
                .then()
                .statusCode(200);
    }

    @Test
    void shouldCreate2FA_forAValidNewSecondFactorPasscodeRequest_withProvisionalTrue() throws JsonProcessingException {
        databaseHelper.updateProvisionalOtpKey(username, "ABCDEFGHIJKLMNOP");

        Map<String, Boolean> body = Map.of("provisional", true);

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(body))
                .post(format(USER_2FA_URL, externalId))
                .then()
                .statusCode(200);
    }

    @Test
    void shouldReturnBadRequest_forAValidNewSecondFactorPasscodeRequest_withProvisionalTrue_ifNoProvisionalOtpKey() throws JsonProcessingException {
        Map<String, Boolean> body = Map.of("provisional", true);

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(body))
                .post(format(USER_2FA_URL, externalId))
                .then()
                .statusCode(400)
                .body("errors", is(List.of(format("Attempted to send a 2FA token attempted for user without an OTP key [%s]", externalId))));
    }

    @Test
    void shouldAuthenticate2FA_forAValid2FAAuthRequest() throws Exception {
        GoogleAuthenticator testAuthenticator = new GoogleAuthenticator();
        int passcode = testAuthenticator.getTotpPassword(OTP_KEY);
        Map<String, Integer> authBody = Map.of("code", passcode);

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
    void shouldReturnNotFound_forNonExistentUser_when2FAAuthCreateRequest() {
        String nonExistingExternalId = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        givenSetup()
                .when()
                .accept(JSON)
                .post(format(USER_2FA_URL, nonExistingExternalId))
                .then()
                .statusCode(404);
    }

    @Test
    void shouldReturnUnauthorized_onInvalid2FACode_during2FAAuth() throws Exception {
        int invalidPasscode = 111111;
        Map<String, Integer> authBody = Map.of("code", invalidPasscode);

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(authBody))
                .post(format(USER_2FA_AUTHENTICATE_URL, externalId))
                .then()
                .statusCode(401);
    }

    @Test
    void shouldReturnUnauthorizedAndAccountLocked_during2FAAuth_ifMaxRetryExceeded() throws Exception {
        databaseHelper.updateLoginCount(username, 10);

        int invalidPasscode = 111111;
        Map<String, Integer> authBody = Map.of("code", invalidPasscode);

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
