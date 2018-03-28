package uk.gov.pay.adminusers.resources;

import org.junit.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.valueOf;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;
import static uk.gov.pay.adminusers.resources.UserResource.USERS_RESOURCE;

public class UserResourceGetMultipleTest extends IntegrationTest {

    @Test
    public void shouldReturnMultipleUsers_whenGetUsersWithMultipleIds() throws Exception {
        String gatewayAccount1 = valueOf(nextInt());
        String gatewayAccount2 = valueOf(nextInt());
        Service service = serviceDbFixture(databaseHelper).withGatewayAccountIds(gatewayAccount1, gatewayAccount2).insertService();
        String serviceExternalId = service.getExternalId();
        Role role = roleDbFixture(databaseHelper).insertRole();
        String username1 = randomUuid();
        String email1 = username1 + "@example.com";
        User user1 = userDbFixture(databaseHelper).withServiceRole(service.getId(), role.getId()).withUsername(username1).withEmail(email1).insertUser();
        String username2 = randomUuid();
        String email2 = username2 + "@example.com";
        User user2 = userDbFixture(databaseHelper).withServiceRole(service.getId(), role.getId()).withUsername(username2).withEmail(email2).insertUser();

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .get(USERS_RESOURCE + "?ids=" + user1.getExternalId() + "," + user2.getExternalId())
                .then()
                .statusCode(200)
                .body("[0].external_id", is(user1.getExternalId()))
                .body("[0].username", is(user1.getUsername()))
                .body("[0].password", nullValue())
                .body("[0].email", is(user1.getEmail()))
                .body("[0].service_roles", hasSize(1))
                .body("[0].service_roles[0].service.external_id", is(serviceExternalId))
                .body("[0].service_roles[0].service.name", is(service.getName()))
                .body("[0].telephone_number", is(user1.getTelephoneNumber()))
                .body("[0].otp_key", is(user1.getOtpKey()))
                .body("[0].login_counter", is(0))
                .body("[0].disabled", is(false))
                .body("[0].service_roles[0].role.name", is(role.getName()))
                .body("[0].service_roles[0].role.description", is(role.getDescription()))
                .body("[0].service_roles[0].role.permissions", hasSize(role.getPermissions().size()))
                .body("[0]._links", hasSize(1))
                .body("[0]._links[0].href", is("http://localhost:8080/v1/api/users/" + user1.getExternalId()))
                .body("[0]._links[0].method", is("GET"))
                .body("[0]._links[0].rel", is("self"))
                .body("[1].external_id", is(user2.getExternalId()))
                .body("[1].username", is(user2.getUsername()))
                .body("[1].password", nullValue())
                .body("[1].email", is(user2.getEmail()))
                .body("[1].service_roles", hasSize(1))
                .body("[1].service_roles[0].service.external_id", is(serviceExternalId))
                .body("[1].service_roles[0].service.name", is(service.getName()))
                .body("[1].telephone_number", is(user2.getTelephoneNumber()))
                .body("[1].otp_key", is(user2.getOtpKey()))
                .body("[1].login_counter", is(0))
                .body("[1].disabled", is(false))
                .body("[1].service_roles[0].role.name", is(role.getName()))
                .body("[1].service_roles[0].role.description", is(role.getDescription()))
                .body("[1].service_roles[0].role.permissions", hasSize(role.getPermissions().size()))
                .body("[1]._links", hasSize(1))
                .body("[1]._links[0].href", is("http://localhost:8080/v1/api/users/" + user2.getExternalId()))
                .body("[1]._links[0].method", is("GET"))
                .body("[1]._links[0].rel", is("self"))
                .body("size()", is(2));
    }

    @Test
    public void shouldReturnSingleUser_whenGetUsersWithSingleId() throws Exception {
        String gatewayAccount1 = valueOf(nextInt());
        String gatewayAccount2 = valueOf(nextInt());
        Service service = serviceDbFixture(databaseHelper).withGatewayAccountIds(gatewayAccount1, gatewayAccount2).insertService();
        String serviceExternalId = service.getExternalId();
        Role role = roleDbFixture(databaseHelper).insertRole();
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withServiceRole(service.getId(), role.getId()).withUsername(username).withEmail(email).insertUser();

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .get(USERS_RESOURCE + "?ids=" + user.getExternalId())
                .then()
                .statusCode(200)
                .body("[0].external_id", is(user.getExternalId()))
                .body("[0].username", is(user.getUsername()))
                .body("[0].password", nullValue())
                .body("[0].email", is(user.getEmail()))
                .body("[0].service_roles", hasSize(1))
                .body("[0].service_roles[0].service.external_id", is(serviceExternalId))
                .body("[0].service_roles[0].service.name", is(service.getName()))
                .body("[0].telephone_number", is(user.getTelephoneNumber()))
                .body("[0].otp_key", is(user.getOtpKey()))
                .body("[0].login_counter", is(0))
                .body("[0].disabled", is(false))
                .body("[0].service_roles[0].role.name", is(role.getName()))
                .body("[0].service_roles[0].role.description", is(role.getDescription()))
                .body("[0].service_roles[0].role.permissions", hasSize(role.getPermissions().size()))
                .body("[0]._links", hasSize(1))
                .body("[0]._links[0].href", is("http://localhost:8080/v1/api/users/" + user.getExternalId()))
                .body("[0]._links[0].method", is("GET"))
                .body("[0]._links[0].rel", is("self"))
                .body("size()", is(1));
    }

    @Test
    public void shouldReturn404_whenGetUsers_withNonExistentExternalIds() throws Exception {
        givenSetup()
                .when()
                .accept(JSON)
                .get(USERS_RESOURCE + "?ids=NON-EXISTENT-USER,ANOTHER_NON_EXISTENT_USER")
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldReturn404_whenGetUsers_whereSomeNonExistentExternalIds() throws Exception {
        String gatewayAccount1 = valueOf(nextInt());
        String gatewayAccount2 = valueOf(nextInt());
        Service service = serviceDbFixture(databaseHelper).withGatewayAccountIds(gatewayAccount1, gatewayAccount2).insertService();
        Role role = roleDbFixture(databaseHelper).insertRole();
        String username = randomUuid();
        String email = username + "@example.com";
        User existingUser = userDbFixture(databaseHelper).withServiceRole(service.getId(), role.getId()).withUsername(username).withEmail(email).insertUser();

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .get(USERS_RESOURCE + "?ids=" + existingUser.getExternalId() + ",NON-EXISTENT-USER")
                .then()
                .statusCode(404);
    }
}

