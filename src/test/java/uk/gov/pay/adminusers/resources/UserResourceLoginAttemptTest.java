package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class UserResourceLoginAttemptTest extends UserResourceTestBase {

    @Test
    public void shouldLockAccount_onTooManyInvalidAttempts() throws Exception {
        String random = randomUUID().toString();
        createAValidUser(random);
        String username = "user-" + random;
        databaseTestHelper.updateLoginCount(username, 3);

        ImmutableMap<Object, Object> authPayload = ImmutableMap.builder()
                .put("username", username)
                .put("password", "invalid-password")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(authPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_AUTHENTICATE_URL)
                .then()
                .statusCode(423)
                .body("errors", hasSize(1))
                .body("errors[0]", is(format("user [%s] locked due to too many login attempts", username)));

    }

    @Test
    public void shouldIncreaseLoginCount_whenRecordLoginAttempt() throws Exception {
        String random = randomUUID().toString();
        createAValidUser(random);
        String username = "user-" + random;

        givenSetup()
                .when()
                .post(format(LOGIN_ATTEMPT_URL, username))
                .then()
                .statusCode(200);
    }

    @Test
    public void shouldErrorLocked_whenRecordLoginAttempt_ifAttemptsMoreThanAllowed() throws Exception {
        String random = randomUUID().toString();
        createAValidUser(random);
        String username = "user-" + random;
        databaseTestHelper.updateLoginCount(username, 3);

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .post(format(LOGIN_ATTEMPT_URL, username))
                .then()
                .statusCode(423)
                .body("errors", hasSize(1))
                .body("errors[0]", is(format("user [%s] locked due to too many login attempts", username)));
    }

    @Test
    public void shouldError404_whenRecordLoginAttempt_onEmptyUserName() throws Exception {

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .post(format(LOGIN_ATTEMPT_URL, ""))
                .then()
                .statusCode(404);
    }
}
