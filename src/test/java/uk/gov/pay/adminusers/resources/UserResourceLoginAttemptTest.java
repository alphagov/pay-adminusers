package uk.gov.pay.adminusers.resources;

import org.junit.Test;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class UserResourceLoginAttemptTest extends UserResourceTestBase {

    @Test
    public void shouldIncreaseLoginCount_whenRecordLoginAttempt() throws Exception {

        String username = createAValidUser();

        givenSetup()
                .when()
                .post(format(LOGIN_ATTEMPT_URL, username))
                .then()
                .statusCode(200)
                .body("username", is(username))
                .body("login_counter", is(1));
    }

    @Test
    public void shouldErrorLocked_whenRecordLoginAttempt_ifAttemptsMoreThanAllowed() throws Exception {

        String username = createAValidUser();
        databaseTestHelper.updateLoginCount(username, 10);

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .post(format(LOGIN_ATTEMPT_URL, username))
                .then()
                .statusCode(401)
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

    @Test
    public void shouldError400_whenResetLoginAttemptActionIsInvalid() throws Exception {

        String username = createAValidUser();

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .post(format(LOGIN_ATTEMPT_URL, username) + "?action=invalidate")
                .then()
                .statusCode(400)
                .body("errors", hasSize(1))
                .body("errors[0]", is("Parameter [action] value is invalid"));
    }

    @Test
    public void shouldError200_whenResetLoginAttemptActionIsValid() throws Exception {

        String username = createAValidUser();

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .post(format(LOGIN_ATTEMPT_URL, username) + "?action=reset")
                .then()
                .statusCode(200);
    }
}
