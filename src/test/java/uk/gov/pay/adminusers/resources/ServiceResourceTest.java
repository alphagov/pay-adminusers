package uk.gov.pay.adminusers.resources;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;

import java.util.List;
import java.util.Map;

import static com.jayway.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;
import static uk.gov.pay.adminusers.resources.ServiceResource.HEADER_USER_CONTEXT;

public class ServiceResourceTest extends IntegrationTest {

    private int serviceId;
    private String serviceExternalId;
    private User userWithRoleAdminInService1;
    private User user1WithRoleViewInService1;
    private User user2WithRoleViewInService1;

    @Before
    public void setup() {

        Role roleAdmin = roleDbFixture(databaseHelper).insertAdmin();
        Role roleView = roleDbFixture(databaseHelper)
                .withName("roleView")
                .insertRole();
        Service service = serviceDbFixture(databaseHelper).insertService();
        serviceExternalId = service.getExternalId();
        serviceId = service.getId();

        userWithRoleAdminInService1 = userDbFixture(databaseHelper)
                .withServiceRole(service, roleAdmin.getId())
                .withUsername("c" + RandomStringUtils.random(10))
                .insertUser();

        user1WithRoleViewInService1 = userDbFixture(databaseHelper)
                .withServiceRole(service, roleView.getId())
                .withUsername("b" + RandomStringUtils.random(10))
                .insertUser();

        user2WithRoleViewInService1 = userDbFixture(databaseHelper)
                .withServiceRole(service, roleView.getId())
                .withUsername("a" + RandomStringUtils.random(10))
                .insertUser();
    }

    @Deprecated // remove when support for serviceId is taken off
    @Test
    public void getUsers_shouldReturnListOfAllUsersWithRolesForAGivenServiceOrderedByUsername_usingServiceId() {

        givenSetup()
                .when()
                .accept(JSON)
                .get(String.format("/v1/api/services/%s/users", serviceId))
                .then()
                .statusCode(200)
                .body("$", hasSize(3))
                .body("[0].username", is(user2WithRoleViewInService1.getUsername()))
                .body("[0]._links", hasSize(1))
                .body("[0]._links[0].href", is("http://localhost:8080/v1/api/users/" + user2WithRoleViewInService1.getExternalId()))
                .body("[0]._links[0].method", is("GET"))
                .body("[0]._links[0].rel", is("self"))
                .body("[1].username", is(user1WithRoleViewInService1.getUsername()))
                .body("[1]._links", hasSize(1))
                .body("[1]._links[0].href", is("http://localhost:8080/v1/api/users/" + user1WithRoleViewInService1.getExternalId()))
                .body("[1]._links[0].method", is("GET"))
                .body("[1]._links[0].rel", is("self"))
                .body("[2].username", is(userWithRoleAdminInService1.getUsername()))
                .body("[2]._links", hasSize(1))
                .body("[2]._links[0].href", is("http://localhost:8080/v1/api/users/" + userWithRoleAdminInService1.getExternalId()))
                .body("[2]._links[0].method", is("GET"))
                .body("[2]._links[0].rel", is("self"));
    }

    @Test
    public void shouldReturnListOfAllUsersWithRolesForAGivenServiceOrderedByUsername_identifiedByExternalid() {
        Role role1 = roleDbFixture(databaseHelper)
                .withName("role-"+randomUuid())
                .insertRole();
        Role role2 = roleDbFixture(databaseHelper)
                .withName("role-"+randomUuid())
                .insertRole();

        Service service1 = serviceDbFixture(databaseHelper).insertService();
        Service service2 = serviceDbFixture(databaseHelper).insertService();

        User user1 = userDbFixture(databaseHelper)
                .withUsername("zoe-"+randomUuid())
                .withServiceRole(service1.getId(), role1.getId()).insertUser();
        User user2 = userDbFixture(databaseHelper)
                .withUsername("tim-"+randomUuid())
                .withServiceRole(service1.getId(), role2.getId()).insertUser();
        User user3 = userDbFixture(databaseHelper)
                .withUsername("bob-"+randomUuid())
                .withServiceRole(service1.getId(), role2.getId()).insertUser();

        userDbFixture(databaseHelper)
                .withServiceRole(service2.getId(), role1.getId()).insertUser();

        givenSetup()
                .when()
                .accept(JSON)
                .get(String.format("/v1/api/services/%s/users", service1.getExternalId()))
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
                .delete(String.format("/v1/api/services/%s/users/%s", serviceExternalId, user1WithRoleViewInService1.getExternalId()))
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
                .delete(String.format("/v1/api/services/%s/users/%s", serviceExternalId, userWithRoleAdminInService1.getExternalId()))
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
                .delete(String.format("/v1/api/services/%s/users/%s", serviceExternalId, userWithRoleAdminInService1.getExternalId()))
                .then()
                .statusCode(403)
                .body(isEmptyString());
    }

    @Test
    public void removeServiceUser_shouldReturnForbiddenWhenUserContextHeaderIsMissing() {

        givenSetup()
                .when()
                .accept(JSON)
                .delete(String.format("/v1/api/services/%s/users/%s", serviceExternalId, userWithRoleAdminInService1.getExternalId()))
                .then()
                .statusCode(403)
                .body(isEmptyString());
    }
}
