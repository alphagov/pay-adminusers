package uk.gov.pay.adminusers.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;

import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class ToolboxEndpointResourceIT extends IntegrationTest {

    private User user;
    private Role role;
    private String serviceExternalId;
    
    @BeforeEach
    public void setup() {
        role = roleDbFixture(databaseHelper).withName("roleView").insertRole();
        
        Service service = serviceDbFixture(databaseHelper).insertService();
        serviceExternalId = service.getExternalId();

        String username = "b" + randomUuid();
        String email = username + "@example.com";
        user = userDbFixture(databaseHelper)
                .withServiceRole(service, role.getId())
                .withEmail(email)
                .insertUser();
    }
    
    @Test
    public void should_remove_user_from_service() {
        List<Map<String, Object>> serviceRoleForUserBefore = databaseHelper.findServiceRoleForUser(user.getId());
        assertThat(serviceRoleForUserBefore.size(), is(1));

        givenSetup()
                .when()
                .accept(JSON)
                .get(format("/v1/api/services/%s/users", serviceExternalId))
                .then()
                .body("$", hasItem(allOf(hasEntry("username", user.getUsername()))));

        givenSetup()
                .when()
                .accept(JSON)
                .delete(format("/v1/api/toolbox/services/%s/users/%s", serviceExternalId, user.getExternalId()))
                .then()
                .statusCode(204)
                .body(emptyString());

        List<Map<String, Object>> serviceRoleForUserAfter = databaseHelper.findServiceRoleForUser(user.getId());
        assertThat(serviceRoleForUserAfter.isEmpty(), is(true));

        givenSetup()
                .when()
                .accept(JSON)
                .get(format("/v1/api/services/%s/users", serviceExternalId))
                .then()
                .body("$", not(hasItem(allOf(hasEntry("username", user.getUsername())))));
    }
}
