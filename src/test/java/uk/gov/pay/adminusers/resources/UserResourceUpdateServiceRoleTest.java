package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class UserResourceUpdateServiceRoleTest extends IntegrationTest {

    @Test
    public void shouldUpdateUserServiceRole() {
        Role role = roleDbFixture(databaseHelper).insertAdmin();
        Service service = serviceDbFixture(databaseHelper).insertService();
        String serviceExternalId = service.getExternalId();
        String username1 = randomUuid();
        String email1 = username1 + "@example.com";
        User user = userDbFixture(databaseHelper).withServiceRole(service, role.getId()).withUsername(username1).withEmail(email1).insertUser();
        String username2 = randomUuid();
        String email2 = username2 + "@example.com";
        userDbFixture(databaseHelper).withServiceRole(service, role.getId()).withUsername(username2).withEmail(email2).insertUser();

        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("role_name", "view-and-refund"));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .put(format(USER_SERVICE_RESOURCE, user.getExternalId(), serviceExternalId))
                .then()
                .statusCode(200)
                .body("username", is(user.getUsername()))
                .body("service_roles[0].role.name", is("view-and-refund"))
                .body("service_roles[0].role.description", is("View and Refund"));
    }

    @Test
    public void shouldError404_ifUserNotFound_whenUpdatingServiceRole() {
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
    public void shouldError412_ifNoOfMinimumAdminsLimitReached_whenUpdatingServiceRole() {
        Role role = roleDbFixture(databaseHelper).insertAdmin();
        Service service = serviceDbFixture(databaseHelper).insertService();
        String serviceExternalId = service.getExternalId();
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withServiceRole(service, role.getId()).withUsername(username).withEmail(email).insertUser();

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
