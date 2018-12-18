package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static com.jayway.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.ForgottenPasswordDbFixture.forgottenPasswordDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class ResetPasswordResourceTest extends IntegrationTest {

    private static final String RESET_PASSWORD_RESOURCE_URL = "/v1/api/reset-password";
    private static final String CURRENT_PASSWORD = "myOldEncryptedPassword";

    private int userId;

    @Before
    public void before() {
        String username = randomUuid();
        String email = username + "@example.com";
        userId = userDbFixture(databaseHelper).withPassword(CURRENT_PASSWORD).withUsername(username).withEmail(email).insertUser().getId();
    }

    @Test
    public void resetPassword_shouldReturn204_whenCodeIsValid_changingTheOldPasswordToTheNewEncryptedOne() throws Exception {

        String forgottenPasswordCode = forgottenPasswordDbFixture(databaseHelper, userId).insertForgottenPassword();
        String password = "iPromiseIWon'tForgetThisPassword";

        ImmutableMap<Object, Object> payload = ImmutableMap.builder()
                .put("forgotten_password_code", forgottenPasswordCode)
                .put("new_password", password)
                .build();

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

        ImmutableMap<Object, Object> payload = ImmutableMap.builder()
                .put("forgotten_password_code", "aCodeThatDoesNotExist")
                .put("new_password", "iPromiseIWon'tForgetThisPassword")
                .build();

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


        String expiredForgottenPasswordCode = forgottenPasswordDbFixture(databaseHelper, userId).expired().insertForgottenPassword();

        ImmutableMap<Object, Object> payload = ImmutableMap.builder()
                .put("forgotten_password_code", expiredForgottenPasswordCode)
                .put("new_password", "iPromiseIWon'tForgetThisPassword")
                .build();

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
