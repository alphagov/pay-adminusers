package uk.gov.pay.adminusers.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;
import static uk.gov.pay.adminusers.resources.ServiceResource.HEADER_USER_CONTEXT;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ServiceResourceIT extends IntegrationTest {

    private String serviceExternalId;
    private User userWithRoleAdminInService1;
    private User user1WithRoleViewInService1;
    private Role roleView;

    @BeforeEach
    public void setUp() {
        Role roleAdmin = roleDbFixture(databaseHelper).insertAdmin();
        roleView = roleDbFixture(databaseHelper)
                .withName("roleView")
                .insertRole();
        Service service = serviceDbFixture(databaseHelper).insertService();
        serviceExternalId = service.getExternalId();

        String email1 = "c" + randomUuid() + "@example.com";
        userWithRoleAdminInService1 = userDbFixture(databaseHelper)
                .withServiceRole(service, roleAdmin.getId())
                .withEmail(email1)
                .insertUser();

        String email2 = "b" + randomUuid() + "@example.com";
        user1WithRoleViewInService1 = userDbFixture(databaseHelper)
                .withServiceRole(service, roleView.getId())
                .withEmail(email2)
                .insertUser();

        String email3 = "a" + randomUuid() + "@example.com";
        userDbFixture(databaseHelper)
                .withServiceRole(service, roleView.getId())
                .withEmail(email3)
                .insertUser();
    }
    
    @Nested
    class GetUsersForService {

        @Test
        public void should_return_users_ordered_by_email() {
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

            String email1 = "zoe-" + randomUuid() + "@example.com";
            User user1 = userDbFixture(databaseHelper)
                    .withEmail(email1)
                    .withServiceRole(service1.getId(), role1.getId()).insertUser();
            String email2 = "tim-" + randomUuid() + "@example.com";
            User user2 = userDbFixture(databaseHelper)
                    .withEmail(email2)
                    .withServiceRole(service1.getId(), role2.getId()).insertUser();
            String email3 = "bob-" + randomUuid() + "@example.com";
            User user3 = userDbFixture(databaseHelper)
                    .withEmail(email3)
                    .withServiceRole(service1.getId(), role2.getId()).insertUser();

            String email4 = randomUuid() + "@example.com";
            userDbFixture(databaseHelper)
                    .withEmail(email4)
                    .withServiceRole(service2.getId(), role1.getId()).insertUser();

            givenSetup()
                    .when()
                    .accept(JSON)
                    .get(format("/v1/api/services/%s/users", service1.getExternalId()))
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(3))
                    .body("[0].email", is(user3.getEmail()))
                    .body("[0]._links", hasSize(1))
                    .body("[0]._links[0].href", is("http://localhost:8080/v1/api/users/" + user3.getExternalId()))
                    .body("[0]._links[0].method", is("GET"))
                    .body("[0]._links[0].rel", is("self"))
                    .body("[1].email", is(user2.getEmail()))
                    .body("[1]._links", hasSize(1))
                    .body("[1]._links[0].href", is("http://localhost:8080/v1/api/users/" + user2.getExternalId()))
                    .body("[1]._links[0].method", is("GET"))
                    .body("[1]._links[0].rel", is("self"))
                    .body("[2].email", is(user1.getEmail()))
                    .body("[2]._links", hasSize(1))
                    .body("[2]._links[0].href", is("http://localhost:8080/v1/api/users/" + user1.getExternalId()))
                    .body("[2]._links[0].method", is("GET"))
                    .body("[2]._links[0].rel", is("self"));
        }

        @Test
        public void should_return_404_if_service_does_not_exist() {
            givenSetup()
                    .when()
                    .accept(JSON)
                    .get("/v1/api/services/999/users")
                    .then()
                    .statusCode(404);
        }
    }

    @Test
    public void get_service_by_external_id() {
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
    
    @Nested
    class DeleteUsersFromService {

        @Test
        public void should_delete_user_from_service_successfully() {
            List<Map<String, Object>> serviceRoleForUserBefore = databaseHelper.findServiceRoleForUser(user1WithRoleViewInService1.getId());
            assertThat(serviceRoleForUserBefore.size(), is(1));

            givenSetup()
                    .when()
                    .accept(JSON)
                    .get(format("/v1/api/services/%s/users", serviceExternalId))
                    .then()
                    .body("$", hasItem(allOf(hasEntry("email", user1WithRoleViewInService1.getEmail()))));

            givenSetup()
                    .when()
                    .accept(JSON)
                    .header(HEADER_USER_CONTEXT, userWithRoleAdminInService1.getExternalId())
                    .delete(format("/v1/api/services/%s/users/%s", serviceExternalId, user1WithRoleViewInService1.getExternalId()))
                    .then()
                    .statusCode(204)
                    .body(emptyString());

            List<Map<String, Object>> serviceRoleForUserAfter = databaseHelper.findServiceRoleForUser(user1WithRoleViewInService1.getId());
            assertThat(serviceRoleForUserAfter.isEmpty(), is(true));

            givenSetup()
                    .when()
                    .accept(JSON)
                    .get(format("/v1/api/services/%s/users", serviceExternalId))
                    .then()
                    .body("$", not(hasItem(allOf(hasEntry("email", user1WithRoleViewInService1.getEmail())))));
        }

        @Test
        public void should_delete_user_from_specified_service_only() {
            Service anotherService = serviceDbFixture(databaseHelper).insertService();

            databaseHelper.addUserServiceRole(user1WithRoleViewInService1.getId(), anotherService.getId(), roleView.getId());

            givenSetup()
                    .when()
                    .accept(JSON)
                    .get(format("/v1/api/services/%s/users", serviceExternalId))
                    .then()
                    .body("$", hasItem(allOf(hasEntry("email", user1WithRoleViewInService1.getEmail()))));

            givenSetup()
                    .when()
                    .accept(JSON)
                    .get(format("/v1/api/services/%s/users", anotherService.getExternalId()))
                    .then()
                    .body("$", hasItem(allOf(hasEntry("email", user1WithRoleViewInService1.getEmail()))));

            givenSetup()
                    .when()
                    .accept(JSON)
                    .header(HEADER_USER_CONTEXT, userWithRoleAdminInService1.getExternalId())
                    .delete(format("/v1/api/services/%s/users/%s", serviceExternalId, user1WithRoleViewInService1.getExternalId()))
                    .then()
                    .statusCode(204)
                    .body(emptyString());

            givenSetup()
                    .when()
                    .accept(JSON)
                    .get(format("/v1/api/services/%s/users", serviceExternalId))
                    .then()
                    .body("$", not(hasItem(allOf(hasEntry("email", user1WithRoleViewInService1.getEmail())))));

            givenSetup()
                    .when()
                    .accept(JSON)
                    .get(format("/v1/api/services/%s/users", anotherService.getExternalId()))
                    .then()
                    .body("$", hasItem(allOf(hasEntry("email", user1WithRoleViewInService1.getEmail()))));
        }

        @Test
        public void should_return_409_if_user_tries_to_delete_itself() {
            givenSetup()
                    .when()
                    .accept(JSON)
                    .header(HEADER_USER_CONTEXT, userWithRoleAdminInService1.getExternalId())
                    .delete(format("/v1/api/services/%s/users/%s", serviceExternalId, userWithRoleAdminInService1.getExternalId()))
                    .then()
                    .statusCode(409)
                    .body(emptyString());
        }

        @Test
        public void should_return_403_if_GovUkPay_User_Context_Header_is_empty() {
            givenSetup()
                    .when()
                    .accept(JSON)
                    .header(HEADER_USER_CONTEXT, " ")
                    .delete(format("/v1/api/services/%s/users/%s", serviceExternalId, userWithRoleAdminInService1.getExternalId()))
                    .then()
                    .statusCode(403)
                    .body(emptyString());
        }

        @Test
        public void should_return_403_if_GovUkPay_User_Context_Header_is_missing() {
            givenSetup()
                    .when()
                    .accept(JSON)
                    .delete(format("/v1/api/services/%s/users/%s", serviceExternalId, userWithRoleAdminInService1.getExternalId()))
                    .then()
                    .statusCode(403)
                    .body(emptyString());
        }
    }
}
