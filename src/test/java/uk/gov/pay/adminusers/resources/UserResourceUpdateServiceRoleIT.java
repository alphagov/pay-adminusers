package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class UserResourceUpdateServiceRoleIT extends IntegrationTest {

    private Role adminRole;

    @BeforeEach
    void setUp() throws Exception {
        RoleDao roleDao = getInjector().getInstance(RoleDao.class);
        adminRole = roleDao.findByRoleName(RoleName.ADMIN).get().toRole();
    }
    
    @Test
    public void shouldUpdateUserServiceRole() {
        Service service = serviceDbFixture(databaseHelper).insertService();
        String serviceExternalId = service.getExternalId();
        String email1 = randomUuid() + "@example.com";
        User user = userDbFixture(databaseHelper).withServiceRole(service, adminRole).withEmail(email1).insertUser();
        String email2 = randomUuid() + "@example.com";
        userDbFixture(databaseHelper).withServiceRole(service, adminRole).withEmail(email2).insertUser();

        JsonNode payload = mapper.valueToTree(Map.of("role_name", "view-and-refund"));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .put(format("/v1/api/users/%s/services/%s", user.getExternalId(), serviceExternalId))
                .then()
                .statusCode(200)
                .body("email", is(user.getEmail()))
                .body("service_roles[0].role.name", is("view-and-refund"))
                .body("service_roles[0].role.description", is("View and Refund"));
    }

    @Test
    public void shouldError404_ifUserNotFound_whenUpdatingServiceRole() {
        String serviceExternalId = serviceDbFixture(databaseHelper).insertService().getExternalId();
        JsonNode payload = mapper.valueToTree(Map.of("role_name", "view-and-refund"));

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
        Service service = serviceDbFixture(databaseHelper).insertService();
        String serviceExternalId = service.getExternalId();
        String email = randomUuid() + "@example.com";
        User user = userDbFixture(databaseHelper).withServiceRole(service, adminRole).withEmail(email).insertUser();

        JsonNode payload = mapper.valueToTree(Map.of("role_name", "view-and-refund"));

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
