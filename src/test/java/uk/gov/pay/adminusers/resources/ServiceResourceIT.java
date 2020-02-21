package uk.gov.pay.adminusers.resources;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;

import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;
import static uk.gov.pay.adminusers.resources.ServiceResource.HEADER_USER_CONTEXT;

public class ServiceResourceIT extends IntegrationTest {

    private String serviceExternalId;
    private User userWithRoleAdminInService1;
    private User user1WithRoleViewInService1;

    @Before
    public void setUp() {
        Role roleAdmin = roleDbFixture(databaseHelper).insertAdmin();
        Role roleView = roleDbFixture(databaseHelper)
                .withName("roleView")
                .insertRole();
        Service service = serviceDbFixture(databaseHelper).insertService();
        serviceExternalId = service.getExternalId();

        String username1 = "c" + randomUuid();
        String email1 = username1 + "@example.com";
        userWithRoleAdminInService1 = userDbFixture(databaseHelper)
                .withServiceRole(service, roleAdmin.getId())
                .withUsername(username1)
                .withEmail(email1)
                .insertUser();

        String username2 = "b" + randomUuid();
        String email2 = username2 + "@example.com";
        user1WithRoleViewInService1 = userDbFixture(databaseHelper)
                .withServiceRole(service, roleView.getId())
                .withUsername(username2)
                .withEmail(email2)
                .insertUser();

        String username3 = "a" + randomUuid();
        String email3 = username3 + "@example.com";
        userDbFixture(databaseHelper)
                .withServiceRole(service, roleView.getId())
                .withUsername(username3)
                .withEmail(email3)
                .insertUser();
    }

    @Test
    public void shouldReturnListOfAllUsersWithRolesForAGivenServiceOrderedByUsername_identifiedByExternalid() {
        Role role1 = roleDbFixture(databaseHelper)
                .withName("role-" + randomUuid())
                .insertRole();
        Role role2 = roleDbFixture(databaseHelper)
                .withName("role-" + randomUuid())
                .insertRole();

        Service service1 = serviceDbFixture(databaseHelper)
                .withExperimentalFeaturesEnabled(true)
                .insertService();
        Service service2 = serviceDbFixture(databaseHelper)
                .withExperimentalFeaturesEnabled(false)
                .insertService();

        String username1 = "zoe-" + randomUuid();
        String email1 = username1 + "@example.com";
        User user1 = userDbFixture(databaseHelper)
                .withUsername(username1)
                .withEmail(email1)
                .withServiceRole(service1.getId(), role1.getId()).insertUser();
        String username2 = "tim-" + randomUuid();
        String email2 = username2 + "@example.com";
        User user2 = userDbFixture(databaseHelper)
                .withUsername(username2)
                .withEmail(email2)
                .withServiceRole(service1.getId(), role2.getId()).insertUser();
        String username3 = "bob-" + randomUuid();
        String email3 = username3 + "@example.com";
        User user3 = userDbFixture(databaseHelper)
                .withUsername(username3)
                .withEmail(email3)
                .withServiceRole(service1.getId(), role2.getId()).insertUser();

        String username4 = randomUuid();
        String email4 = username4 + "@example.com";
        userDbFixture(databaseHelper)
                .withUsername(username4)
                .withEmail(email4)
                .withServiceRole(service2.getId(), role1.getId()).insertUser();

        givenSetup()
                .when()
                .accept(JSON)
                .get(format("/v1/api/services/%s/users", service1.getExternalId()))
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
    public void shouldReturnAGivenService_identifiedByExternalid() {
        Service service1 = serviceDbFixture(databaseHelper)
                .withExperimentalFeaturesEnabled(true)
                .insertService();
        
        givenSetup()
                .when()
                .accept(JSON)
                .get(format("/v1/api/services/%s/", service1.getExternalId()))
                .then()
                .statusCode(200)
                .body("experimental_features_enabled", is(true));
    }

    @Test
    public void getServiceUsers_shouldReturn404WhenServiceDoesNotExist() {
        givenSetup()
                .when()
                .accept(JSON)
                .get("/v1/api/services/999/users")
                .then()
                .statusCode(404);
    }

    @Test
    public void removeServiceUser_shouldRemoveAnUserFromAService() {
        List<Map<String, Object>> serviceRoleForUserBefore = databaseHelper.findServiceRoleForUser(user1WithRoleViewInService1.getId());
        assertThat(serviceRoleForUserBefore.size(), is(1));

        givenSetup()
                .when()
                .accept(JSON)
                .header(HEADER_USER_CONTEXT, userWithRoleAdminInService1.getExternalId())
                .delete(format("/v1/api/services/%s/users/%s", serviceExternalId, user1WithRoleViewInService1.getExternalId()))
                .then()
                .statusCode(204)
                .body(isEmptyString());

        List<Map<String, Object>> serviceRoleForUserAfter = databaseHelper.findServiceRoleForUser(user1WithRoleViewInService1.getId());
        assertThat(serviceRoleForUserAfter.isEmpty(), is(true));
    }

    @Test
    public void removeServiceUser_shouldNotBeAbleToRemoveAnUserItself() {
        givenSetup()
                .when()
                .accept(JSON)
                .header(HEADER_USER_CONTEXT, userWithRoleAdminInService1.getExternalId())
                .delete(format("/v1/api/services/%s/users/%s", serviceExternalId, userWithRoleAdminInService1.getExternalId()))
                .then()
                .statusCode(409)
                .body(isEmptyString());
    }

    @Test
    public void removeServiceUser_shouldReturnForbiddenWhenRemoverIsMissing() {
        givenSetup()
                .when()
                .accept(JSON)
                .header(HEADER_USER_CONTEXT, " ")
                .delete(format("/v1/api/services/%s/users/%s", serviceExternalId, userWithRoleAdminInService1.getExternalId()))
                .then()
                .statusCode(403)
                .body(isEmptyString());
    }

    @Test
    public void removeServiceUser_shouldReturnForbiddenWhenUserContextHeaderIsMissing() {
        givenSetup()
                .when()
                .accept(JSON)
                .delete(format("/v1/api/services/%s/users/%s", serviceExternalId, userWithRoleAdminInService1.getExternalId()))
                .then()
                .statusCode(403)
                .body(isEmptyString());
    }
}
