package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.fixtures.ForgottenPasswordDbFixture;
import uk.gov.pay.adminusers.fixtures.UserDbFixture;

import java.util.Map;

import static com.jayway.restassured.http.ContentType.JSON;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ResetPasswordResourceTest extends IntegrationTest {

    private static final String RESET_PASSWORD_RESOURCE_URL = "/v1/api/reset-password";
    private static final String CURRENT_PASSWORD = "myOldEncryptedPassword";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private int userId;

    @Before
    public void before() throws Exception {
        userId = UserDbFixture.aUser(databaseTestHelper).withPassword(CURRENT_PASSWORD).build().getId();
    }

    @Test
    public void resetPassword_shouldReturn204_whenCodeIsValid_changingTheOldPasswordToTheNewEncryptedOne() throws Exception {

        String forgottenPasswordCode = ForgottenPasswordDbFixture.aForgottenPassword(databaseTestHelper, userId).build();
        String password = "iPromiseIWon'tForgetThisPassword";

        ImmutableMap<Object, Object> payload = ImmutableMap.builder()
                .put("forgotten_password_code", forgottenPasswordCode)
                .put("new_password", password)
                .build();

        givenSetup()
                .when()
                .body(MAPPER.writeValueAsString(payload))
                .contentType(JSON)
                .accept(JSON)
                .post(RESET_PASSWORD_RESOURCE_URL)
                .then()
                .statusCode(204);

        Map<String, Object> userAttributes = databaseTestHelper.findUser(userId).get(0);
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
                .body(MAPPER.writeValueAsString(payload))
                .contentType(JSON)
                .accept(JSON)
                .post(RESET_PASSWORD_RESOURCE_URL)
                .then()
                .statusCode(404)
                .body("errors", hasSize(1))
                .body("errors[0]", is("Field [forgotten_password_code] non-existent/expired"));

        Map<String, Object> userAttributes = databaseTestHelper.findUser(userId).get(0);
        Object userPassword = userAttributes.get("password");

        assertThat(userPassword, is(CURRENT_PASSWORD));
    }

    @Test
    public void resetPassword_shouldReturn400_whenCodeHasExpired_andCurrentEncryptedPasswordShouldNotChange() throws Exception {


        String expiredForgottenPasswordCode= ForgottenPasswordDbFixture.aForgottenPassword(databaseTestHelper, userId).expired().build();

        ImmutableMap<Object, Object> payload = ImmutableMap.builder()
                .put("forgotten_password_code", expiredForgottenPasswordCode)
                .put("new_password", "iPromiseIWon'tForgetThisPassword")
                .build();

        givenSetup()
                .when()
                .body(MAPPER.writeValueAsString(payload))
                .contentType(JSON)
                .accept(JSON)
                .post(RESET_PASSWORD_RESOURCE_URL)
                .then()
                .statusCode(404)
                .body("errors", hasSize(1))
                .body("errors[0]", is("Field [forgotten_password_code] non-existent/expired"));

        Map<String, Object> userAttributes = databaseTestHelper.findUser(userId).get(0);
        Object userPassword = userAttributes.get("password");

        assertThat(userPassword, is(CURRENT_PASSWORD));
    }

    @Test
    public void resetPassword_shouldReturn400_whenJsonIsMissing() throws Exception {

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
    public void resetPassword_shouldReturn400_whenFieldsAreMissing() throws Exception {

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
