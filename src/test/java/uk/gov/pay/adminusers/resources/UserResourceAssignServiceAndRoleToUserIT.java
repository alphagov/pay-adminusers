package uk.gov.pay.adminusers.resources;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UserResourceAssignServiceAndRoleToUserIT extends IntegrationTest {

    private Role adminRole;

    @BeforeEach
    void setUp() throws Exception {
        RoleDao roleDao = getInjector().getInstance(RoleDao.class);
        adminRole = roleDao.findByRoleName(RoleName.ADMIN).get().toRole();
    }
    
    @Test
    void should_assign_service_and_role_to_user_successfully() {
        Service service = serviceDbFixture(databaseHelper).insertService();
        String email = randomUuid() + "@example.com";
        User user = userDbFixture(databaseHelper).withEmail(email).insertUser();

        givenSetup()
                .when()
                .contentType(JSON)
                .body(Map.of("service_external_id", service.getExternalId(),"role_name", adminRole.getRoleName().getName()))
                .post(format("/v1/api/users/%s/services", user.getExternalId()))
                .then()
                .statusCode(200)
                .body("email", is(user.getEmail()))
                .body("service_roles", hasSize(1))
                .body("service_roles[0].role.name", is(adminRole.getRoleName().getName()))
                .body("service_roles[0].service.external_id", is(service.getExternalId()));
    }

    @Test
    void should_error_when_service_external_id_is_missing() {
        serviceDbFixture(databaseHelper).insertService();
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withEmail(email).insertUser();

        givenSetup()
                .when()
                .contentType(JSON)
                .body(Map.of("role_name", adminRole.getRoleName().getName()))
                .post(format("/v1/api/users/%s/services", user.getExternalId()))
                .then()
                .statusCode(422)
                .body("errors", Matchers.hasSize(1))
                .body("errors[0]", containsString("Field [service_external_id] is required"));
    }

    @Test
    void should_error_when_role_name_is_missing() {
        Service service = serviceDbFixture(databaseHelper).insertService();
        User user = userDbFixture(databaseHelper).withEmail(randomUuid() + "@example.com").insertUser();

        givenSetup()
                .when()
                .contentType(JSON)
                .body(Map.of("service_external_id", service.getExternalId()))
                .post(format("/v1/api/users/%s/services", user.getExternalId()))
                .then()
                .statusCode(422)
                .body("errors", Matchers.hasSize(1))
                .body("errors[0]", is("Field [role_name] must be one of 'admin', 'view-and-refund', 'view-only', 'view-and-initiate-moto', and 'view-refund-and-initiate-moto'"));
    }

    @Test
    void should_error_when_service_and_role_are_already_assigned_to_user() {
        String roleName = "view-and-refund";
        Service service = serviceDbFixture(databaseHelper).insertService();
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withServiceRole(service.getId(), adminRole).withEmail(email).insertUser();

        givenSetup()
                .when()
                .contentType(JSON)
                .body(Map.of("service_external_id", service.getExternalId(), "role_name", roleName))
                .post(format("/v1/api/users/%s/services", user.getExternalId()))
                .then()
                .statusCode(409)
                .body("errors", Matchers.hasSize(1))
                .body("errors[0]", is(format("Cannot assign service role. user [%s] already got access to service [%s].", user.getExternalId(), service.getExternalId())));
    }
}
