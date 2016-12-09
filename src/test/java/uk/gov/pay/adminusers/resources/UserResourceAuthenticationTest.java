package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class UserResourceAuthenticationTest extends UserResourceTestBase {

    @Test
    public void shouldAuthenticateUser_onAValidUsernamePasswordCombination() throws Exception {
        String random = randomUUID().toString();
        createAValidUser(random);

        ImmutableMap<Object, Object> authPayload = ImmutableMap.builder()
                .put("username", "user-" + random)
                .put("password", "password-" + random)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(authPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_AUTHENTICATE_URL)
                .then()
                .statusCode(200)
                .body("username", is("user-" + random))
                .body("email", is("user-" + random + "@example.com"))
                .body("gateway_account_id", is("1"))
                .body("telephone_number", is("45334534634"))
                .body("otp_key", is("34f34"))
                .body("login_count", is(0))
                .body("disabled", is(false))
                .body("_links", hasSize(1))
                .body("role.name", is("admin"))
                .body("permissions", hasSize(27)); //we could consider removing this assertion if the permissions constantly changing

    }

    @Test
    public void shouldAuthenticateFail_onAInvalidUsernamePasswordCombination() throws Exception {
        String random = randomUUID().toString();
        createAValidUser(random);

        ImmutableMap<Object, Object> authPayload = ImmutableMap.builder()
                .put("username", "user-" + random)
                .put("password", "invalid-password")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(authPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_AUTHENTICATE_URL)
                .then()
                .statusCode(401)
                .body("errors", hasSize(1))
                .body("errors[0]", is("invalid username and/or password"));

    }

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
                .statusCode(401)
                .body("errors", hasSize(1))
                .body("errors[0]", is(format("user [%s] locked due to too many login attempts", username)));

    }

}
