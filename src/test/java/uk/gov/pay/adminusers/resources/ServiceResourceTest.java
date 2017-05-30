package uk.gov.pay.adminusers.resources;

import org.junit.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.User;

import static com.jayway.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class ServiceResourceTest extends IntegrationTest {

    @Test
    public void shouldReturnListOfAllUsersWithRolesForAGivenServiceOrderedByUsername() {
        Role role1 = roleDbFixture(databaseHelper)
                .withName("roleB")
                .insertRole();
        Role role2 = roleDbFixture(databaseHelper)
                .withName("roleA")
                .insertRole();

        int serviceId1 = serviceDbFixture(databaseHelper).insertService().getId();
        int serviceId2 = serviceDbFixture(databaseHelper).insertService().getId();

        User user1 = userDbFixture(databaseHelper)
                .withUsername("zoe")
                .withServiceRole(serviceId1, role1.getId()).insertUser();
        User user2 = userDbFixture(databaseHelper)
                .withUsername("tim")
                .withServiceRole(serviceId1, role2.getId()).insertUser();
        User user3 = userDbFixture(databaseHelper)
                .withUsername("bob")
                .withServiceRole(serviceId1, role2.getId()).insertUser();

        userDbFixture(databaseHelper)
                .withServiceRole(serviceId2, role1.getId()).insertUser();

        givenSetup()
                .when()
                .accept(JSON)
                .get(String.format("/v1/api/services/%s/users", serviceId1))
                .then()
                .statusCode(200)
                .body("$", hasSize(3))
                .body("[0].username", is(user3.getUsername()))
                .body("[0]._links", hasSize(1))
                .body("[0]._links[0].href", is("http://localhost:8080/v1/api/users/" + user3.getExternalId()))
                .body("[0]._links[0].method", is("GET"))
                .body("[0]._links[0].rel", is("self"))
                .body("[1].username", is(user2.getUsername()))
                .body("[1]._links", hasSize(1))
                .body("[1]._links[0].href", is("http://localhost:8080/v1/api/users/" + user2.getExternalId()))
                .body("[1]._links[0].method", is("GET"))
                .body("[1]._links[0].rel", is("self"))
                .body("[2].username", is(user1.getUsername()))
                .body("[2]._links", hasSize(1))
                .body("[2]._links[0].href", is("http://localhost:8080/v1/api/users/" + user1.getExternalId()))
                .body("[2]._links[0].method", is("GET"))
                .body("[2]._links[0].rel", is("self"));
    }

    @Test
    public void shouldReturn404WhenServiceDoesNotExist() {
        givenSetup()
                .when()
                .accept(JSON)
                .get("/v1/api/services/999/users")
                .then()
                .statusCode(404);
    }
}
