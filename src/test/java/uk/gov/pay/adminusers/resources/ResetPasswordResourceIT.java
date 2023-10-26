package uk.gov.pay.adminusers.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.ForgottenPasswordDbFixture.aForgottenPasswordDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class ResetPasswordResourceIT extends IntegrationTest {

    private static final String RESET_PASSWORD_RESOURCE_URL = "/v1/api/reset-password";
    private static final String CURRENT_PASSWORD = "myOldEncryptedPassword";

    private int userId;

    @BeforeEach
    public void before() {
        String username = randomUuid();
        String email = username + "@example.com";
        userId = userDbFixture(databaseHelper).withPassword(CURRENT_PASSWORD).withEmail(email).insertUser().getId();
    }

    @Test
    public void resetPassword_shouldReturn204_whenCodeIsValid_changingTheOldPasswordToTheNewEncryptedOne() throws Exception {

        String forgottenPasswordCode = aForgottenPasswordDbFixture()
                .withDatabaseTestHelper(databaseHelper)
                .withUserId(userId)
                .insert()
                .getForgottenPasswordCode();

        String password = "iPromiseIWon'tForgetThisPassword";

        Map<Object, Object> payload = Map.of(
                "forgotten_password_code", forgottenPasswordCode,
                "new_password", password);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .contentType(JSON)
                .accept(JSON)
                .post(RESET_PASSWORD_RESOURCE_URL)
                .then()
                .statusCode(204);

        Map<String, Object> userAttributes = databaseHelper.findUser(userId).get(0);
        Object userPassword = userAttributes.get("password");

        assertThat(userPassword, is(notNullValue()));
        assertThat(userPassword, is(not(password)));
        assertThat(userPassword, is(not(CURRENT_PASSWORD)));
    }

    @Test
    public void resetPassword_shouldReturn400_whenCodeIsInvalid_andCurrentEncryptedPasswordShouldNotChange() throws Exception {

        Map<Object, Object> payload = Map.of(
                "forgotten_password_code", "aCodeThatDoesNotExist",
                "new_password", "iPromiseIWon'tForgetThisPassword");

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .contentType(JSON)
                .accept(JSON)
                .post(RESET_PASSWORD_RESOURCE_URL)
                .then()
                .statusCode(404)
                .body("errors", hasSize(1))
                .body("errors[0]", is("Field [forgotten_password_code] non-existent/expired"));

        Map<String, Object> userAttributes = databaseHelper.findUser(userId).get(0);
        Object userPassword = userAttributes.get("password");

        assertThat(userPassword, is(CURRENT_PASSWORD));
    }

    @Test
    public void resetPassword_shouldReturn400_whenCodeHasExpired_andCurrentEncryptedPasswordShouldNotChange() throws Exception {

        ZonedDateTime expired = ZonedDateTime.now(ZoneId.of("UTC")).minus(91, MINUTES);
        String expiredForgottenPasswordCode = aForgottenPasswordDbFixture()
                .withDatabaseTestHelper(databaseHelper)
                .withUserId(userId)
                .withExpiryDate(expired)
                .insert()
                .getForgottenPasswordCode();

        Map<Object, Object> payload = Map.of(
                "forgotten_password_code", expiredForgottenPasswordCode,
                "new_password", "iPromiseIWon'tForgetThisPassword");

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .contentType(JSON)
                .accept(JSON)
                .post(RESET_PASSWORD_RESOURCE_URL)
                .then()
                .statusCode(404)
                .body("errors", hasSize(1))
                .body("errors[0]", is("Field [forgotten_password_code] non-existent/expired"));

        Map<String, Object> userAttributes = databaseHelper.findUser(userId).get(0);
        Object userPassword = userAttributes.get("password");

        assertThat(userPassword, is(CURRENT_PASSWORD));
    }

    @Test
    public void resetPassword_shouldReturn400_whenJsonIsMissing() {

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .post(RESET_PASSWORD_RESOURCE_URL)
                .then()
                .statusCode(400)
                .body("errors", hasSize(1))
                .body("errors[0]", is("invalid JSON"));
    }

    @Test
    public void resetPassword_shouldReturn400_whenFieldsAreMissing() {

        givenSetup()
                .when()
                .body("{}")
                .contentType(JSON)
                .accept(JSON)
                .post(RESET_PASSWORD_RESOURCE_URL)
                .then()
                .statusCode(400)
                .body("errors", hasSize(2))
                .body("errors[0]", is("Field [forgotten_password_code] is required"))
                .body("errors[1]", is("Field [new_password] is required"));
    }
}
