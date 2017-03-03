package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.User;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.apache.commons.lang3.RandomStringUtils.*;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.pay.adminusers.model.Role.role;

public class ResetPasswordResourceTest extends IntegrationTest {

    private static final String RESET_PASSWORD_RESOURCE_URL = "/v1/api/reset-password";
    private static final String CURRENT_PASSWORD = "myOldEncryptedPassword";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private String forgottenPasswordCode;
    private int userId;

    @Before
    public void before() throws Exception {
        userId = nextInt();
        forgottenPasswordCode = randomAlphanumeric(255);
        int serviceId = nextInt();
        int roleId = nextInt();
        Role role = role(roleId, "name", "desc");
        Permission permission = Permission.permission(nextInt(), "name", "desc");
        role.setPermissions(newArrayList(permission));
        databaseTestHelper.addService(serviceId, randomNumeric(5));
        databaseTestHelper.add(permission);
        databaseTestHelper.add(role);
        databaseTestHelper.add(aUser(userId, CURRENT_PASSWORD), serviceId, roleId);
        databaseTestHelper.add(aForgottenPassword(forgottenPasswordCode, ZonedDateTime.now(ZoneId.of("UTC"))), userId);
    }

    @Test
    public void resetPassword_shouldReturn204_whenCodeIsValid_changingTheOldPasswordToTheNewEncryptedOne() throws Exception {

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

        String expiredForgottenPasswordCode = "expiredCode";
        databaseTestHelper.add(aForgottenPassword(expiredForgottenPasswordCode, ZonedDateTime.now(ZoneId.of("UTC")).minus(91, MINUTES)), userId);

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

    private ForgottenPassword aForgottenPassword(String random, ZonedDateTime date) {
        return ForgottenPassword.forgottenPassword(nextInt(), random, format("%s-name", random), date);
    }

    private User aUser(int id, String encryptedPassword) {
        String username = randomAlphabetic(20);
        return User.from(id, username, encryptedPassword, username + "@example.com", "1", "784rh", "8948924");
    }
}
