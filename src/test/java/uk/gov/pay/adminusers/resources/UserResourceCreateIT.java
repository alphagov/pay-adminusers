package uk.gov.pay.adminusers.resources;

import io.restassured.response.ValidatableResponse;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Service;

import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class UserResourceCreateIT extends IntegrationTest {

    @Test
    public void shouldCreateAUser_Successfully() throws Exception {
        String username = randomUuid();
        Map<Object, Object> userPayload = Map.of(
                "username", username,
                "email", "user-" + username + "@example.com",
                "telephone_number", "+441134960000",
                "otp_key", "34f34",
                "role_name", "admin");

        ValidatableResponse response = givenSetup().when()
                .body(mapper.writeValueAsString(userPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_RESOURCE_URL)
                .then();

        String externalId = response.extract().path("external_id");

        response
                .statusCode(201)
                .body("id", nullValue())
                .body("external_id", is(externalId))
                .body("username", is(username))
                .body("password", nullValue())
                .body("email", is("user-" + username + "@example.com"))
                .body("service_roles", hasSize(0))
                .body("telephone_number", is("+441134960000"))
                .body("otp_key", is("34f34"))
                .body("login_counter", is(0))
                .body("disabled", is(false));

        response
                .body("_links", hasSize(1))
                .body("_links[0].href", is("http://localhost:8080/v1/api/users/" + externalId))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("self"));

        //TODO - WIP This will be removed when PP-1612 is done.
        // This is an extra check to verify that new created user gateways are registered withing the new Services Model as well as in users table
        List<Map<String, Object>> userByExternalId = databaseHelper.findUserByExternalId(externalId);
        List<Map<String, Object>> servicesAssociatedToUser = databaseHelper.findUserServicesByUserId((Integer) userByExternalId.get(0).get("id"));
        assertThat(servicesAssociatedToUser.size(), is(0));
    }

    @Test
    public void shouldCreateAUser_withinAService_IfServiceExternalIdsExists() throws Exception {
        String gatewayAccount1 = valueOf(nextInt());
        String gatewayAccount2 = valueOf(nextInt());
        Service service = serviceDbFixture(databaseHelper).withGatewayAccountIds(gatewayAccount1, gatewayAccount2).insertService();
        String serviceExternalId = service.getExternalId();
        String username = randomUuid();

        Map<Object, Object> userPayload = Map.of(
                "username", username,
                "email", "user-" + username + "@example.com",
                "service_external_ids", new String[]{valueOf(serviceExternalId)},
                "telephone_number", "+441134960000",
                "otp_key", "34f34",
                "role_name", "admin");

        ValidatableResponse response = givenSetup().when()
                .body(mapper.writeValueAsString(userPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_RESOURCE_URL)
                .then();

        String externalId = response.extract().path("external_id");

        response
                .statusCode(201)
                .body("id", nullValue())
                .body("external_id", is(externalId))
                .body("username", is(username))
                .body("password", nullValue())
                .body("email", is("user-" + username + "@example.com"))
                .body("service_roles", hasSize(1))
                .body("service_roles[0].service.external_id", is(serviceExternalId))
                .body("service_roles[0].service.name", is(service.getName()))
                .body("telephone_number", is("+441134960000"))
                .body("otp_key", is("34f34"))
                .body("login_counter", is(0))
                .body("disabled", is(false))
                .body("service_roles[0].role.name", is("admin"))
                .body("service_roles[0].role.description", is("Administrator"))
                .body("service_roles[0].role.permissions", hasSize(47));

        response
                .body("_links", hasSize(1))
                .body("_links[0].href", is("http://localhost:8080/v1/api/users/" + externalId))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("self"));

        //TODO - WIP This will be removed when PP-1612 is done.
        // This is an extra check to verify that new created user gateways are registered withing the new Services Model as well as in users table
        List<Map<String, Object>> userByExternalId = databaseHelper.findUserByExternalId(externalId);
        List<Map<String, Object>> servicesAssociatedToUser = databaseHelper.findUserServicesByUserId((Integer) userByExternalId.get(0).get("id"));
        assertThat(servicesAssociatedToUser.size(), is(1));
    }

    @Test
    public void shouldError400_IfRoleDoesNotExist() throws Exception {
        String username = randomUuid();
        Map<Object, Object> userPayload = Map.of(
                "username", username,
                "email", "user-" + username + "@example.com",
                "gateway_account_ids", new String[]{"1", "2"},
                "telephone_number", "01134960000",
                "otp_key", "34f34",
                "role_name", "invalid-role");

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(userPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_RESOURCE_URL)
                .then()
                .statusCode(400)
                .body("errors", hasSize(1))
                .body("errors[0]", is("role [invalid-role] not recognised"));
    }

    @Test
    public void shouldError400_whenFieldsMissingForUserCreation() throws Exception {
        Map<Object, Object> invalidPayload = emptyMap();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invalidPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_RESOURCE_URL)
                .then()
                .statusCode(400)
                .body("errors", hasSize(4))
                .body("errors", hasItems(
                        "Field [username] is required",
                        "Field [email] is required",
                        "Field [telephone_number] is required",
                        "Field [role_name] is required"));
    }

    @Test
    public void shouldError409_IfUsernameAlreadyExists() throws Exception {
        String gatewayAccount = valueOf(nextInt());
        serviceDbFixture(databaseHelper).withGatewayAccountIds(gatewayAccount).insertService();
        String username = randomUuid();
        String email = username + "@example.com";
        userDbFixture(databaseHelper).withUsername(username).withEmail(email).insertUser();

        Map<Object, Object> userPayload = Map.of(
                "username", username,
                "email", email,
                "gateway_account_ids", new String[]{gatewayAccount},
                "telephone_number", "01134960000",
                "role_name", "admin");

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(userPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_RESOURCE_URL)
                .then()
                .statusCode(409)
                .body("errors", hasSize(1))
                .body("errors[0]", is(format("username [%s] already exists", username)));
    }

}

