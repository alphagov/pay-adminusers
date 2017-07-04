package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class UserResourceUpdateServiceRoleTest extends IntegrationTest {

    @Test
    public void shouldUpdateUserServiceRole() throws Exception {
        Role role = roleDbFixture(databaseHelper).insertAdmin();
        Service service = serviceDbFixture(databaseHelper).insertService();
        String serviceExternalId = service.getExternalId();
        User user = userDbFixture(databaseHelper).withServiceRole(service, role.getId()).insertUser();
        userDbFixture(databaseHelper).withServiceRole(service, role.getId()).insertUser();

        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("role_name", "view-and-refund"));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .put(format(USER_SERVICE_RESOURCE, user.getExternalId(), serviceExternalId))
                .then()
                .statusCode(200)
                .body("username", is(user.getUsername()))
                .body("role.name", is("view-and-refund"))
                .body("role.description", is("View and Refund"));
    }

    @Test
    public void shouldError404_ifUserNotFound_whenUpdatingServiceRole() throws Exception {
        String serviceExternalId = serviceDbFixture(databaseHelper).insertService().getExternalId();
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("role_name", "view-and-refund"));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .put(format(USER_SERVICE_RESOURCE, "non-existent", serviceExternalId))
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldError412_ifNoOfMinimumAdminsLimitReached_whenUpdatingServiceRole() throws Exception {
        Role role = roleDbFixture(databaseHelper).insertAdmin();
        Service service = serviceDbFixture(databaseHelper).insertService();
        String serviceExternalId = service.getExternalId();
        User user = userDbFixture(databaseHelper).withServiceRole(service, role.getId()).insertUser();

        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("role_name", "view-and-refund"));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .put(format(USER_SERVICE_RESOURCE, user.getExternalId(), serviceExternalId))
                .then()
                .statusCode(412)
                .body("errors", hasSize(1))
                .body("errors[0]", is("Service admin limit reached. At least 1 admin(s) required"));
    }
}
