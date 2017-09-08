package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.ValidatableResponse;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class UserResourceCreateAndGetTest extends IntegrationTest {

    @Test
    public void shouldCreateAUser_Successfully() throws Exception {

        String username = randomAlphanumeric(10) + randomUUID().toString();
        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", username)
                .put("email", "user-" + username + "@example.com")
                .put("telephone_number", "45334534634")
                .put("otp_key", "34f34")
                .put("role_name", "admin")
                .build();

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
                .body("telephone_number", is("45334534634"))
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
        String username = randomAlphanumeric(10) + randomUUID().toString();

        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", username)
                .put("email", "user-" + username + "@example.com")
                .put("service_external_ids", new String[]{valueOf(serviceExternalId)})
                .put("telephone_number", "45334534634")
                .put("otp_key", "34f34")
                .put("role_name", "admin")
                .build();

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
                .body("telephone_number", is("45334534634"))
                .body("otp_key", is("34f34"))
                .body("login_counter", is(0))
                .body("disabled", is(false))
                .body("service_roles[0].role.name", is("admin"))
                .body("service_roles[0].role.description", is("Administrator"))
                .body("service_roles[0].role.permissions", hasSize(31));

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
    public void shouldReturnUser_whenGetUserWithExternalId() throws Exception {
        String gatewayAccount1 = valueOf(nextInt());
        String gatewayAccount2 = valueOf(nextInt());
        Service service = serviceDbFixture(databaseHelper).withGatewayAccountIds(gatewayAccount1, gatewayAccount2).insertService();
        String serviceExternalId = service.getExternalId();
        Role role = roleDbFixture(databaseHelper).insertRole();
        User user = userDbFixture(databaseHelper).withServiceRole(service.getId(), role.getId()).insertUser();

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .get(format(USER_RESOURCE_URL, user.getExternalId()))
                .then()
                .statusCode(200)
                .body("external_id", is(user.getExternalId()))
                .body("username", is(user.getUsername()))
                .body("password", nullValue())
                .body("email", is(user.getEmail()))
                .body("service_roles", hasSize(1))
                .body("service_roles[0].service.external_id", is(serviceExternalId))
                .body("service_roles[0].service.name", is(service.getName()))
                .body("telephone_number", is(user.getTelephoneNumber()))
                .body("otp_key", is(user.getOtpKey()))
                .body("login_counter", is(0))
                .body("disabled", is(false))
                .body("service_roles[0].role.name", is(role.getName()))
                .body("service_roles[0].role.description", is(role.getDescription()))
                .body("service_roles[0].role.permissions", hasSize(role.getPermissions().size()))
                .body("_links", hasSize(1))
                .body("_links[0].href", is("http://localhost:8080/v1/api/users/" + user.getExternalId()))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("self"));
    }


    @Test
    public void shouldError400_IfRoleDoesNotExist() throws Exception {
        String username = randomAlphanumeric(10) + randomUUID().toString();
        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", username)
                .put("email", "user-" + username + "@example.com")
                .put("gateway_account_ids", new String[]{"1", "2"})
                .put("telephone_number", "45334534634")
                .put("otp_key", "34f34")
                .put("role_name", "invalid-role")
                .build();

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
        ImmutableMap<Object, Object> invalidPayload = ImmutableMap.builder().build();

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
        String username = userDbFixture(databaseHelper).insertUser().getUsername();

        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", username)
                .put("email", "user-" + username + "@example.com")
                .put("gateway_account_ids", new String[]{gatewayAccount})
                .put("telephone_number", "45334534634")
                .put("role_name", "admin")
                .build();

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


    @Test
    public void shouldReturn404_whenGetUser_withNonExistentUsername() throws Exception {
        givenSetup()
                .when()
                .accept(JSON)
                .get(format(USER_RESOURCE_URL, "non-existent-user"))
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldReturn200_IfExternalIdsDoExist() throws Exception {
        String gatewayAccount1 = valueOf(nextInt());
        String gatewayAccount2 = valueOf(nextInt());
        Service service = serviceDbFixture(databaseHelper).withGatewayAccountIds(gatewayAccount1, gatewayAccount2).insertService();
        String serviceExternalId = service.getExternalId();
        Role role = roleDbFixture(databaseHelper).insertRole();
        User user = userDbFixture(databaseHelper).withServiceRole(service.getId(), role.getId()).insertUser();
        User user2 = userDbFixture(databaseHelper).withServiceRole(service.getId(), role.getId()).insertUser();
        User user3 = userDbFixture(databaseHelper).withServiceRole(service.getId(), role.getId()).insertUser();

        List<String> externalIds = new ArrayList<>();
        externalIds.add(user.getExternalId());
        externalIds.add(user2.getExternalId());
        externalIds.add(user3.getExternalId());

        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("external_ids", externalIds)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(userPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USER_EMAILS_RESOURCE_URL)
                .then()
                .statusCode(200)
                .body("results.size()", is(3))
                .body("results[0].email", notNullValue())
                .body("results[0].external_id", notNullValue())
                .body("results[2].email", notNullValue())
                .body("results[2].external_id", notNullValue());

    }

    @Test
    public void shouldReturn404_whenGetUser_withInvalidMaxLengthUsername() throws Exception {
        givenSetup()
                .when()
                .accept(JSON)
                .get(format(USER_RESOURCE_URL, RandomStringUtils.randomAlphanumeric(256)))
                .then()
                .statusCode(404);
    }
}

