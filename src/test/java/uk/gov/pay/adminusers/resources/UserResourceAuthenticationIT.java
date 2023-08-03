package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.fixtures.UserDbFixture;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.service.PasswordHasher;

import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;

public class UserResourceAuthenticationIT extends IntegrationTest {

    @Test
    public void shouldAuthenticateUser_onAValidUsernamePasswordCombination() throws Exception {
        String[] gatewayAccountIds = {"1", "2"};
        Service service = serviceDbFixture(databaseHelper).withGatewayAccountIds(gatewayAccountIds).insertService();

        String email = createAValidUser(service);

        Map<Object, Object> authPayload = Map.of(
                "email", email,
                "password", "password-" + email);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(authPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_AUTHENTICATE_URL)
                .then()
                .statusCode(200)
                .body("email", is(email))
                .body("service_roles", hasSize(1))
                .body("service_roles[0].service.external_id", is(notNullValue()))
                .body("service_roles[0].service.name", is(notNullValue()))
                .body("telephone_number", is("+441134960000"))
                .body("otp_key", is("34f34"))
                .body("login_counter", is(0))
                .body("disabled", is(false))
                .body("_links", hasSize(1))
                .body("service_roles[0].role.name", is("admin"))
                .body("service_roles[0].role.permissions.size()", greaterThan(1));
    }

    @Test
    public void shouldAuthenticateUser_onAValidUsernamePasswordCombination_whenUserDoesNotBelongToAService() throws Exception {
        String email = randomUuid() + "@example.com";
        String password = "password-" + email;
        String encryptedPassword = new PasswordHasher().hash(password);

        UserDbFixture.userDbFixture(databaseHelper)
                .withEmail(email)
                .withPassword(encryptedPassword).insertUser();

        Map<Object, Object> authPayload = Map.of(
                "email", email,
                "password", password);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(authPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_AUTHENTICATE_URL)
                .then()
                .statusCode(200)
                .body("email", is(email))
                .body("service_roles", hasSize(0))
                .body("_links", hasSize(1));
    }

    @Test
    public void shouldAuthenticateFail_onAInvalidUsernamePasswordCombination() throws Exception {
        String[] gatewayAccountIds = {"3", "4"};
        Service service = serviceDbFixture(databaseHelper).withGatewayAccountIds(gatewayAccountIds).insertService();

        String email = createAValidUser(service);

        Map<Object, Object> authPayload = Map.of(
                "email", email,
                "password", "invalid-password");

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

    private String createAValidUser(Service service) throws JsonProcessingException {
        String email = "user-" + randomAlphanumeric(10) + UUID.randomUUID() + "@example.com";
        Map<Object, Object> userPayload = Map.of(
                "username", email,
                "password", "password-" + email,
                "email", email,
                "service_external_ids", new String[]{service.getExternalId()},
                "telephone_number", "+441134960000",
                "otp_key", "34f34",
                "role_name", "admin");

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(userPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_RESOURCE_URL)
                .then()
                .statusCode(201);

        return email;
    }

}
