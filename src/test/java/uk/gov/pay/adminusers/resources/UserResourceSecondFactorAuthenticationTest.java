package uk.gov.pay.adminusers.resources;

import com.jayway.restassured.response.ValidatableResponse;
import org.junit.Test;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

public class UserResourceSecondFactorAuthenticationTest extends UserResourceTestBase {

    public static final String USER_2FA_AUTH_SUBMIT_URL = USER_2FA_AUTHENTICATE_URL + "/%s";

    @Test
    public void shouldCreateAndAuthenticate2FA_onForAValid2FAAuthRequest() throws Exception {
        String random = randomUUID().toString();
        createAValidUser(random);
        String username = "user-" + random;
        ValidatableResponse validatableResponse = givenSetup()
                .when()
                .accept(JSON)
                .post(format(USER_2FA_AUTHENTICATE_URL, username))
                .then()
                .statusCode(201);

        validatableResponse
                .body("username", is("user-" + random))
                .body("passcode", is(notNullValue()))
                .body("_links", hasSize(1));

        String passcode = validatableResponse.extract().body().jsonPath().get("passcode");

        givenSetup()
                .when()
                .accept(JSON)
                .post(format(USER_2FA_AUTH_SUBMIT_URL, username, passcode))
                .then()
                .statusCode(200)
                .body("username", is("user-" + random))
                .body("login_counter", is(0))
                .body("disabled", is(false));
    }

    @Test
    public void shouldReturnNotFound_forNonExistentUser_when2FAAuthRequest() throws Exception {
        String username = "non-existent";
        givenSetup()
                .when()
                .accept(JSON)
                .post(format(USER_2FA_AUTHENTICATE_URL, username))
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldReturnUnauthorized_onInvalid2FACode_during2FAAuth() throws Exception {
        String random = randomUUID().toString();
        createAValidUser(random);

        String username = "user-" + random;
        String invalidPasscode = "111111";

        givenSetup()
                .when()
                .accept(JSON)
                .post(format(USER_2FA_AUTH_SUBMIT_URL, username, invalidPasscode))
                .then()
                .statusCode(401);

    }

    @Test
    public void shouldReturnUnauthorizedAndAccountLocked_during2FAAuth_ifMaxRetryExceeded() throws Exception {
        String random = randomUUID().toString();
        createAValidUser(random);

        String username = "user-" + random;
        databaseTestHelper.updateLoginCount(username, 10);

        String invalidPasscode = "111111";
        givenSetup()
                .when()
                .accept(JSON)
                .post(format(USER_2FA_AUTH_SUBMIT_URL, username, invalidPasscode))
                .then()
                .statusCode(401);


        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .get(format(USER_RESOURCE_URL, username))
                .then()
                .statusCode(200)
                .body("username", is(username))
                .body("login_counter", is(11))
                .body("disabled", is(true));
    }

}
