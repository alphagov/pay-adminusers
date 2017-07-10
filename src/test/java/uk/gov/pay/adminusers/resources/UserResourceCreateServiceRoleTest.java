package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class UserResourceCreateServiceRoleTest extends IntegrationTest {

    @Test
    public void shouldSuccess_whenAddServiceRoleForUser() throws Exception {
        Role role = roleDbFixture(databaseHelper).insertAdmin();
        Service service = serviceDbFixture(databaseHelper).insertService();
        User user = userDbFixture(databaseHelper).insertUser();

        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("service_external_id", service.getExternalId(), "role_name", role.getName()));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .post(format(USER_SERVICES_RESOURCE, user.getExternalId()))
                .then()
                .statusCode(200)
                .body("username", is(user.getUsername()))
                .body("service_roles", hasSize(1))
                .body("service_roles[0].role.name", is(role.getName()))
                .body("service_roles[0].service.external_id", is(service.getExternalId()));
    }

    @Test
    public void shouldError_whenAddServiceRoleForUser_ifMandatoryParamsMissing() throws Exception {
        Role role = roleDbFixture(databaseHelper).insertAdmin();
        serviceDbFixture(databaseHelper).insertService();
        User user = userDbFixture(databaseHelper).insertUser();

        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("role_name", role.getName()));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .post(format(USER_SERVICES_RESOURCE, user.getExternalId()))
                .then()
                .statusCode(400)
                .body("errors", Matchers.hasSize(1))
                .body("errors[0]", is(format("Field [service_external_id] is required")));
    }

    @Test
    public void shouldError_whenAddServiceRoleForUser_ifUserAlreadyHasService() throws Exception {
        Role role = roleDbFixture(databaseHelper).insertAdmin();
        String roleName = "view-and-refund";
        roleDbFixture(databaseHelper).withName(roleName).insertAdmin();
        Service service = serviceDbFixture(databaseHelper).insertService();
        User user = userDbFixture(databaseHelper).withServiceRole(service.getId(), role.getId()).insertUser();

        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("service_external_id", service.getExternalId(), "role_name", roleName));

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
