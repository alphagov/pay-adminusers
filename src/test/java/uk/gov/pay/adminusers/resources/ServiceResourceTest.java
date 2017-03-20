package uk.gov.pay.adminusers.resources;

import org.junit.Test;
import uk.gov.pay.adminusers.fixtures.RoleDbFixture;
import uk.gov.pay.adminusers.fixtures.ServiceDbFixture;
import uk.gov.pay.adminusers.fixtures.UserDbFixture;
import uk.gov.pay.adminusers.model.Role;

import static com.jayway.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class ServiceResourceTest extends IntegrationTest {

    @Test
    public void shouldReturnListOfAllUsersWithRolesForAGivenServiceOrderedByRoleName() {

        Role role1 = RoleDbFixture
                .aRole(databaseTestHelper)
                .withName("roleB")
                .build();
        Role role2 = RoleDbFixture
                .aRole(databaseTestHelper)
                .withName("roleA")
                .build();

        int serviceId1 = ServiceDbFixture.aService(databaseTestHelper).build();
        int serviceId2 = ServiceDbFixture.aService(databaseTestHelper).build();

        String username1 = UserDbFixture.aUser(databaseTestHelper)
                .withServiceRole(serviceId1, role1.getId()).build().getUsername();

        String username2 = UserDbFixture.aUser(databaseTestHelper)
                .withServiceRole(serviceId1, role2.getId()).build().getUsername();
        String username3 = UserDbFixture.aUser(databaseTestHelper)
                .withServiceRole(serviceId1, role2.getId()).build().getUsername();
        UserDbFixture.aUser(databaseTestHelper)
                .withServiceRole(serviceId2, role1.getId()).build();

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
