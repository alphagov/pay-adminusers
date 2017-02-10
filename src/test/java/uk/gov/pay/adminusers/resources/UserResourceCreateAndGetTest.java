package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import uk.gov.pay.adminusers.model.User;

import java.util.List;
import java.util.Map;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class UserResourceCreateAndGetTest extends UserResourceTestBase {

    @Test
    public void shouldCreateAUserSuccessfully() throws Exception {
        String random = randomUUID().toString();
        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", "user-" + random)
                .put("email", "user-" + random + "@example.com")
                .put("gateway_account_id", "1")
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
                .statusCode(201)
                .body("id", nullValue())
                .body("username", is("user-" + random))
                .body("password", nullValue())
                .body("email", is("user-" + random + "@example.com"))
                .body("gateway_account_id", is("1"))
                .body("telephone_number", is("45334534634"))
                .body("otp_key", is("34f34"))
                .body("login_counter", is(0))
                .body("disabled", is(false))
                .body("_links", hasSize(1))
                .body("_links[0].href", is("http://localhost:8080/v1/api/users/user-" + random))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("self"))
                .body("role.name", is("admin"))
                .body("role.description", is("Administrator"))
                .body("permissions", hasSize(28)); //we could consider removing this assertion if the permissions constantly changing

        //TODO - WIP PP-1483 This will be amended when the story is done.
        // This is an extra check to verify that new created user gateways are registered withing the new Services Model as well as in users table
        List<Map<String, Object>> userByName = databaseTestHelper.findUserByName("user-" + random);
        List<Map<String, Object>> servicesAssociatedToUser = databaseTestHelper.findUserServicesByUserId((Integer) userByName.get(0).get("id"));
        assertThat(servicesAssociatedToUser.size(), is(1));

        List<Map<String, Object>> gatewayAccountsAssociatedToUser = databaseTestHelper.findGatewayAccountsByService((Long) servicesAssociatedToUser.get(0).get("service_id"));
        assertThat(gatewayAccountsAssociatedToUser.size(), is(1));
        assertThat(gatewayAccountsAssociatedToUser.get(0).get("gateway_account_id"), is("1"));
    }

    @Test
    public void shouldAddUserToAServiceWhenCreatingTheUserWithAnAlreadyExistingGatewayAccount() throws Exception {

        int serviceId = 123;
        String gatewayAccount = "666";
        databaseTestHelper.addService(serviceId, gatewayAccount);

        String random = randomUUID().toString();
        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", "user-" + random)
                .put("email", "user-" + random + "@example.com")
                .put("gateway_account_id", gatewayAccount)
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
                .statusCode(201)
                .body("username", is("user-" + random))
                .body("gateway_account_id", is("666"));

        //TODO - WIP PP-1483 This will be amended when the story is done.
        // This is an extra check to verify that new created user gateways are registered withing the new Services Model as well as in users table
        List<Map<String, Object>> userByName = databaseTestHelper.findUserByName("user-" + random);
        List<Map<String, Object>> servicesAssociatedToUser = databaseTestHelper.findUserServicesByUserId((Integer) userByName.get(0).get("id"));
        assertThat(servicesAssociatedToUser.size(), is(1));
        assertThat(servicesAssociatedToUser.get(0).get("service_id"), is(123L));

        List<Map<String, Object>> gatewayAccountsAssociatedToUser = databaseTestHelper.findGatewayAccountsByService((Long) servicesAssociatedToUser.get(0).get("service_id"));
        assertThat(gatewayAccountsAssociatedToUser.size(), is(1));
        assertThat(gatewayAccountsAssociatedToUser.get(0).get("gateway_account_id"), is(gatewayAccount));
    }

    @Test
    public void shouldError400_IfRoleDoesNotExist() throws Exception {
        String random = randomUUID().toString();
        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", "user-" + random)
                .put("email", "user-" + random + "@example.com")
                .put("gateway_account_id", "1")
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
        ImmutableMap<Object, Object> invalidPayload = ImmutableMap.builder()
                .put("gateway_account_id", "1")
                .build();

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

        String random = randomUUID().toString();
        String username = "user-" + random;
        String gatewayAccountId = "2";
        User user = User.from(username, "password", "email@example.com", gatewayAccountId, "otpKey", "3543534");
        int serviceId = RandomUtils.nextInt();
        databaseTestHelper.addService(serviceId, gatewayAccountId);
        databaseTestHelper.add(user, serviceId);

        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", username)
                .put("email", "user-" + random + "@example.com")
                .put("gateway_account_id", gatewayAccountId)
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
    public void shouldReturn404_whenGetUserWithNonExistentUsername() throws Exception {
        givenSetup()
                .when()
                .accept(JSON)
                .get(format(USER_RESOURCE_URL, "non-existent-user"))
                .then()
                .statusCode(404);
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

    @Test
    public void shouldReturnUser_whenGetUserWithUsername() throws Exception {
        String random = randomUUID().toString();
        createAValidUser(random);
        String username = "user-" + random;

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .get(format(USER_RESOURCE_URL, username))
                .then()
                .statusCode(200)
                .body("username", is(username))
                .body("password", nullValue())
                .body("email", is("user-" + random + "@example.com"))
                .body("gateway_account_id", is("1"))
                .body("telephone_number", is("45334534634"))
                .body("otp_key", is("34f34"))
                .body("login_counter", is(0))
                .body("disabled", is(false))
                .body("_links", hasSize(1))
                .body("_links[0].href", is("http://localhost:8080/v1/api/users/user-" + random))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("self"))
                .body("role.name", is("admin"))
                .body("role.description", is("Administrator"))
                .body("permissions", hasSize(28)); //we could consider removing this assertion if the permissions constantly changing


    }
}
