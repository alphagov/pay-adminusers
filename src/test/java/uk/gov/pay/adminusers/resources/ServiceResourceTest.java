package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;

import static com.jayway.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

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

    @Test
    public void getUsers_shouldReturnListOfAllUsersWithRolesForAGivenServiceOrderedByUsername() {

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

        ImmutableMap<Object, Object> userRemoverPayload = ImmutableMap.builder()
                .put("remover_id", userWithRoleAdminInService1.getExternalId())
                .build();

        givenSetup()
                .when()
                .accept(JSON)
                .body(userRemoverPayload)
                .delete(String.format("/v1/api/services/%s/users/%s", serviceExternalId, user1WithRoleViewInService1.getExternalId()))
                .then()
                .statusCode(204)
                .body(isEmptyString());
    }

    @Test
    public void removeServiceUser_shouldNotBeAbleToRemoveAnUserItself() {

        ImmutableMap<Object, Object> userRemoverPayload = ImmutableMap.builder()
                .put("remover_id", userWithRoleAdminInService1.getExternalId())
                .build();

        givenSetup()
                .when()
                .accept(JSON)
                .body(userRemoverPayload)
                .delete(String.format("/v1/api/services/%s/users/%s", serviceExternalId, userWithRoleAdminInService1.getExternalId()))
                .then()
                .statusCode(409)
                .body(isEmptyString());
    }

    @Test
    public void removeServiceUser_shouldReturnBadRequestWhenRemoverIsMissing() {

        ImmutableMap<Object, Object> userRemoverPayload = ImmutableMap.builder()
                .put("remover_id", " ")
                .build();

        givenSetup()
                .when()
                .accept(JSON)
                .body(userRemoverPayload)
                .delete(String.format("/v1/api/services/%s/users/%s", serviceExternalId, userWithRoleAdminInService1.getExternalId()))
                .then()
                .statusCode(400)
                .body(isEmptyString());
    }
}
