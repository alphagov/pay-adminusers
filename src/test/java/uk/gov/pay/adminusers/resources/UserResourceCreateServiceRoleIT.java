package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

class UserResourceCreateServiceRoleIT extends IntegrationTest {

    @Test
    void shouldSuccess_whenAddServiceRoleForUser() {
        Role role = roleDbFixture(databaseHelper).insertAdmin();
        Service service = serviceDbFixture(databaseHelper).insertService();
        String email = randomUuid() + "@example.com";
        User user = userDbFixture(databaseHelper).withEmail(email).insertUser();

        JsonNode payload = mapper.valueToTree(Map.of("service_external_id", service.getExternalId(), "role_name", role.getName()));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .post(format(USER_SERVICES_RESOURCE, user.getExternalId()))
                .then()
                .statusCode(200)
                .body("email", is(user.getEmail()))
                .body("service_roles", hasSize(1))
                .body("service_roles[0].role.name", is(role.getName()))
                .body("service_roles[0].service.external_id", is(service.getExternalId()));
    }

    @Test
    void shouldError_whenAddServiceRoleForUser_ifMandatoryParamsMissing() {
        Role role = roleDbFixture(databaseHelper).insertAdmin();
        serviceDbFixture(databaseHelper).insertService();
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withEmail(email).insertUser();

        JsonNode payload = mapper.valueToTree(Map.of("role_name", role.getName()));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .post(format(USER_SERVICES_RESOURCE, user.getExternalId()))
                .then()
                .statusCode(400)
                .body("errors", Matchers.hasSize(1))
                .body("errors[0]", is("Field [service_external_id] is required"));
    }

    @Test
    void shouldError_whenAddServiceRoleForUser_ifUserAlreadyHasService() {
        Role role = roleDbFixture(databaseHelper).insertAdmin();
        String roleName = "view-and-refund";
        roleDbFixture(databaseHelper).withName(roleName).insertAdmin();
        Service service = serviceDbFixture(databaseHelper).insertService();
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withServiceRole(service.getId(), role.getId()).withEmail(email).insertUser();

        JsonNode payload = mapper.valueToTree(Map.of("service_external_id", service.getExternalId(), "role_name", roleName));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .post(format(USER_SERVICES_RESOURCE, user.getExternalId()))
                .then()
                .statusCode(409)
                .body("errors", Matchers.hasSize(1))
                .body("errors[0]", is(format("Cannot assign service role. user [%s] already got access to service [%s].", user.getExternalId(), service.getExternalId())));
    }
}
