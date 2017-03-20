package uk.gov.pay.adminusers.resources;

import org.junit.Test;
import uk.gov.pay.adminusers.model.Role;

import static com.jayway.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class ServiceResourceTest extends IntegrationTest {

    @Test
    public void shouldReturnListOfAllUsersWithRolesForAGivenServiceOrderedByRoleName() {

        Role role1 = roleDbFixture(databaseHelper)
                .withName("roleB")
                .insertRole();
        Role role2 = roleDbFixture(databaseHelper)
                .withName("roleA")
                .insertRole();

        int serviceId1 = serviceDbFixture(databaseHelper).insertService();
        int serviceId2 = serviceDbFixture(databaseHelper).insertService();

        String username1 = userDbFixture(databaseHelper)
                .withServiceRole(serviceId1, role1.getId()).insertUser().getUsername();

        String username2 = userDbFixture(databaseHelper)
                .withServiceRole(serviceId1, role2.getId()).insertUser().getUsername();
        String username3 = userDbFixture(databaseHelper)
                .withServiceRole(serviceId1, role2.getId()).insertUser().getUsername();
        userDbFixture(databaseHelper)
                .withServiceRole(serviceId2, role1.getId()).insertUser();

        givenSetup()
                .when()
                .accept(JSON)
                .get(String.format("/v1/api/services/%s/users", serviceId1))
                .then()
                .statusCode(200)
                .body("$", hasSize(3))
                .body("[0].username", is(username3))
                .body("[1].username", is(username2))
                .body("[2].username", is(username1));
    }
}
