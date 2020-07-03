package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.User;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class UserResourceSecondFactorProvisioningIT extends IntegrationTest {

    private static final String USER_2FA_PROVISION_URL = USER_2FA_URL + "/provision";
    private static final String USER_2FA_ACTIVATE_URL = USER_2FA_URL + "/activate";

    private static final String ORIGINAL_OTP_KEY = "1111111111111111";

    private String externalId;
    private String username;

    @BeforeEach
    public void createValidUser() {
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withOtpKey(ORIGINAL_OTP_KEY).withUsername(username).withEmail(email).insertUser();

        this.externalId = user.getExternalId();
        this.username = user.getUsername();
    }

    @Test
    public void shouldProvisionNewOtpKey() {
        givenSetup()
                .when()
                .post(format(USER_2FA_PROVISION_URL, externalId))
                .then()
                .statusCode(200)
                .body("username", is(username))
                .body("otp_key", is(ORIGINAL_OTP_KEY))
                .body("provisional_otp_key", is(notNullValue()))
                .body("provisional_otp_key_created_at", is(notNullValue()));
    }

    @Test
    public void shouldReturnNotFoundIfUserNotFoundWhenProvisionNewOtpKey() {
        givenSetup()
                .when()
                .post(format(USER_2FA_PROVISION_URL, "this is not a valid user external ID"))
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldActivateNewOtpKey() throws JsonProcessingException {
        String newOtpKey = givenSetup()
                .when()
                .post(format(USER_2FA_PROVISION_URL, externalId))
                .then().statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .get("provisional_otp_key");

        GoogleAuthenticator testAuthenticator = new GoogleAuthenticator();
        int passCode = testAuthenticator.getTotpPassword(newOtpKey);

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(Map.of("second_factor", "APP", "code", passCode)))
                .post(format(USER_2FA_ACTIVATE_URL, externalId))
                .then()
                .statusCode(200)
                .body("username", is(username))
                .body("second_factor", is("APP"))
                .body("otp_key", is(newOtpKey))
                .body("provisional_otp_key", is(nullValue()))
                .body("provisional_otp_key_created_at", is(nullValue()));
    }

    @Test
    public void shouldReturnBadRequestIfAttemptToActivateNewOtpKeyWithInvalidRequest() throws JsonProcessingException {
        String newOtpKey = givenSetup()
                .when()
                .post(format(USER_2FA_PROVISION_URL, externalId))
                .then().statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .get("provisional_otp_key");

        GoogleAuthenticator testAuthenticator = new GoogleAuthenticator();
        int passCode = testAuthenticator.getTotpPassword(newOtpKey);

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(Map.of("second_factor", "NOT VALID", "code", passCode)))
                .post(format(USER_2FA_ACTIVATE_URL, externalId))
                .then()
                .statusCode(400);
    }

    @Test
    public void shouldReturnUnauthorizedIfAttemptToActivateNewOtpKeyWithIncorrectCode() throws JsonProcessingException {
        String newOtpKey = givenSetup()
                .when()
                .post(format(USER_2FA_PROVISION_URL, externalId))
                .then().statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .get("provisional_otp_key");

        GoogleAuthenticator testAuthenticator = new GoogleAuthenticator();
        int passCode = testAuthenticator.getTotpPassword(newOtpKey);

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(Map.of("second_factor", "APP", "code", passCode + 1)))
                .post(format(USER_2FA_ACTIVATE_URL, externalId))
                .then()
                .statusCode(401);
    }

}
