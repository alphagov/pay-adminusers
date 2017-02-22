package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static com.jayway.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class UserResourceAuthenticationTest extends UserResourceTestBase {

    @Test
    public void shouldAuthenticateUser_onAValidUsernamePasswordCombination() throws Exception {

        String username = createAValidUser();

        ImmutableMap<Object, Object> authPayload = ImmutableMap.builder()
                .put("username", username)
                .put("password", "password-" + username)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(authPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_AUTHENTICATE_URL)
                .then()
                .statusCode(200)
                .body("username", is(username))
                .body("email", is("user-" + username + "@example.com"))
                .body("gateway_account_id", is("1"))
                .body("telephone_number", is("45334534634"))
                .body("otp_key", is("34f34"))
                .body("login_counter", is(0))
                .body("disabled", is(false))
                .body("_links", hasSize(1))
                .body("role.name", is("admin"))
                .body("permissions", hasSize(28)); //we could consider removing this assertion if the permissions constantly changing
    }

    @Test
    public void shouldAuthenticateFail_onAInvalidUsernamePasswordCombination() throws Exception {

        String username = createAValidUser();

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
                .body("errors[0]", is("invalid username and/or password"));

    }

}
