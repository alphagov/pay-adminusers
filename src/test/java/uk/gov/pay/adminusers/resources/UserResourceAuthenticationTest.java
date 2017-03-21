package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.util.UUID;

import static com.jayway.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class UserResourceAuthenticationTest extends IntegrationTest {

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
                .body("gateway_account_ids", hasSize(2))
                .body("gateway_account_ids[0]", is("1"))
                .body("gateway_account_ids[1]", is("2"))
                .body("telephone_number", is("45334534634"))
                .body("otp_key", is("34f34"))
                .body("login_counter", is(0))
                .body("disabled", is(false))
                .body("_links", hasSize(1))
                .body("role.name", is("admin"))
                .body("permissions", hasSize(30)); //we could consider removing this assertion if the permissions constantly changing
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

    private String createAValidUser() throws JsonProcessingException {

        String username = RandomStringUtils.randomAlphanumeric(10) + UUID.randomUUID();
        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", username)
                .put("password", "password-" + username)
                .put("email", "user-" + username + "@example.com")
                .put("gateway_account_ids", new String[]{"1", "2"})
                .put("telephone_number", "45334534634")
                .put("otp_key", "34f34")
                .put("role_name", "admin")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(userPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_RESOURCE_URL)
                .then()
                .statusCode(201);

        return username;
    }
}
