package uk.gov.pay.adminusers.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;

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
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;
import static uk.gov.pay.adminusers.resources.ServiceResource.HEADER_USER_CONTEXT;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ServiceResourceIT extends IntegrationTest {

    private String serviceExternalId;
    private User userWithRoleAdminInService1;
    private User userWithRoleViewInService1;
    private User user2WithRoleViewInService1;
    private Role viewRole;
    private Role adminRole;
    private Service service;

    @BeforeEach
    void setUp() {
        RoleDao roleDao = getInjector().getInstance(RoleDao.class);
        adminRole = roleDao.findByRoleName(RoleName.ADMIN).get().toRole();
        viewRole = roleDao.findByRoleName(RoleName.VIEW_ONLY).get().toRole();
        service = serviceDbFixture(databaseHelper).insertService();
        serviceExternalId = service.getExternalId();

        String email1 = "c" + randomUuid() + "@example.com";
        userWithRoleAdminInService1 = userDbFixture(databaseHelper)
                .withServiceRole(service, adminRole)
                .withEmail(email1)
                .insertUser();

        String email2 = "b" + randomUuid() + "@example.com";
        userWithRoleViewInService1 = userDbFixture(databaseHelper)
                .withServiceRole(service, viewRole)
                .withEmail(email2)
                .insertUser();

        String email3 = "a" + randomUuid() + "@example.com";
        user2WithRoleViewInService1 = userDbFixture(databaseHelper)
                .withServiceRole(service, viewRole)
                .withEmail(email3)
                .insertUser();
    }
    
    @Nested
    class GetUsersForService {

        @Test
        void should_return_view_users_ordered_by_email_when_specifying_view_role_query_param() {
            givenSetup()
                    .when()
                    .accept(JSON)
                    .get(format("/v1/api/services/%s/users?role=view-only", service.getExternalId()))
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(2))
                    .body("[0].email", is(user2WithRoleViewInService1.getEmail()))
                    .body("[0]._links", hasSize(1))
                    .body("[0]._links[0].href", is("http://localhost:8080/v1/api/users/" + user2WithRoleViewInService1.getExternalId()))
                    .body("[0]._links[0].method", is("GET"))
                    .body("[0]._links[0].rel", is("self"))
                    .body("[1].email", is(userWithRoleViewInService1.getEmail()))
                    .body("[1]._links", hasSize(1))
                    .body("[1]._links[0].href", is("http://localhost:8080/v1/api/users/" + userWithRoleViewInService1.getExternalId()))
                    .body("[1]._links[0].method", is("GET"))
                    .body("[1]._links[0].rel", is("self"));
        }
        
        @Test
        void should_return_admin_users_ordered_by_email_when_specifying_admin_role_query_param() {
            String email = "d" + randomUuid() + "@example.com";
            User anotherAdminUser = userDbFixture(databaseHelper)
                    .withServiceRole(service, adminRole)
                    .withEmail(email)
                    .insertUser();

            givenSetup()
                    .when()
                    .accept(JSON)
                    .get(format("/v1/api/services/%s/users?role=admin", service.getExternalId()))
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(2))
                    .body("[0].email", is(userWithRoleAdminInService1.getEmail()))
                    .body("[0]._links", hasSize(1))
                    .body("[0]._links[0].href", is("http://localhost:8080/v1/api/users/" + userWithRoleAdminInService1.getExternalId()))
                    .body("[0]._links[0].method", is("GET"))
                    .body("[0]._links[0].rel", is("self"))
                    .body("[1].email", is(anotherAdminUser.getEmail()))
                    .body("[1]._links", hasSize(1))
                    .body("[1]._links[0].href", is("http://localhost:8080/v1/api/users/" + anotherAdminUser.getExternalId()))
                    .body("[1]._links[0].method", is("GET"))
                    .body("[1]._links[0].rel", is("self"));
        }
        
        @Test
        void should_return_users_ordered_by_email() {
            Service service1 = serviceDbFixture(databaseHelper)
                    .withExperimentalFeaturesEnabled(true)
                    .insertService();
            Service service2 = serviceDbFixture(databaseHelper)
                    .withExperimentalFeaturesEnabled(false)
                    .insertService();

            String email1 = "zoe-" + randomUuid() + "@example.com";
            User user1 = userDbFixture(databaseHelper)
                    .withEmail(email1)
                    .withServiceRole(service1.getId(), adminRole).insertUser();
            String email2 = "tim-" + randomUuid() + "@example.com";
            User user2 = userDbFixture(databaseHelper)
                    .withEmail(email2)
                    .withServiceRole(service1.getId(), viewRole).insertUser();
            String email3 = "bob-" + randomUuid() + "@example.com";
            User user3 = userDbFixture(databaseHelper)
                    .withEmail(email3)
                    .withServiceRole(service1.getId(), viewRole).insertUser();

            String email4 = randomUuid() + "@example.com";
            userDbFixture(databaseHelper)
                    .withEmail(email4)
                    .withServiceRole(service2.getId(), adminRole).insertUser();

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
        void should_return_404_if_service_does_not_exist() {
            givenSetup()
                    .when()
                    .accept(JSON)
                    .get("/v1/api/services/999/users")
                    .then()
                    .statusCode(404);
        }
        
        @Test
        void should_return_400_if_role_does_not_exist() {
            givenSetup()
                    .when()
                    .accept(JSON)
                    .get(format("/v1/api/services/%s/users?role=charlix", service.getExternalId()))
                    .then()
                    .statusCode(400)
                    .body("message", is("query param role must be one of [ADMIN, VIEW_AND_REFUND, VIEW_ONLY, " +
                            "VIEW_AND_INITIATE_MOTO, VIEW_REFUND_AND_INITIATE_MOTO, SUPER_ADMIN]"));
        }

        @Test
        void should_not_return_400_AMBIGUOUS_PATH_ENCODING_when_using_url_encoded_query_parameter() {
            String urlEncodedQueryParam = "AD001043%2F22"; // AD001043/22
            givenSetup()
                    .when()
                    .accept(JSON)
                    .get("/v1/api/services/" + service.getExternalId() + "/users?role=" + urlEncodedQueryParam)
                    .then()
                    .statusCode(400)
                    .body("message", is("query param role must be one of [ADMIN, VIEW_AND_REFUND, VIEW_ONLY, " +
                            "VIEW_AND_INITIATE_MOTO, VIEW_REFUND_AND_INITIATE_MOTO, SUPER_ADMIN]"));
        }
    }

    @Test
    void get_service_by_external_id() {
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
        void should_delete_user_from_service_successfully() {
            List<Map<String, Object>> serviceRoleForUserBefore = databaseHelper.findServiceRoleForUser(userWithRoleViewInService1.getId());
            assertThat(serviceRoleForUserBefore.size(), is(1));

            givenSetup()
                    .when()
                    .accept(JSON)
                    .get(format("/v1/api/services/%s/users", serviceExternalId))
                    .then()
                    .body("$", hasItem(allOf(hasEntry("email", userWithRoleViewInService1.getEmail()))));

            givenSetup()
                    .when()
                    .accept(JSON)
                    .header(HEADER_USER_CONTEXT, userWithRoleAdminInService1.getExternalId())
                    .delete(format("/v1/api/services/%s/users/%s", serviceExternalId, userWithRoleViewInService1.getExternalId()))
                    .then()
                    .statusCode(204)
                    .body(emptyString());

            List<Map<String, Object>> serviceRoleForUserAfter = databaseHelper.findServiceRoleForUser(userWithRoleViewInService1.getId());
            assertThat(serviceRoleForUserAfter.isEmpty(), is(true));

            givenSetup()
                    .when()
                    .accept(JSON)
                    .get(format("/v1/api/services/%s/users", serviceExternalId))
                    .then()
                    .body("$", not(hasItem(allOf(hasEntry("email", userWithRoleViewInService1.getEmail())))));
        }

        @Test
        void should_delete_user_from_specified_service_only() {
            Service anotherService = serviceDbFixture(databaseHelper).insertService();

            databaseHelper.addUserServiceRole(userWithRoleViewInService1.getId(), anotherService.getId(), viewRole.getId());

            givenSetup()
                    .when()
                    .accept(JSON)
                    .get(format("/v1/api/services/%s/users", serviceExternalId))
                    .then()
                    .body("$", hasItem(allOf(hasEntry("email", userWithRoleViewInService1.getEmail()))));

            givenSetup()
                    .when()
                    .accept(JSON)
                    .get(format("/v1/api/services/%s/users", anotherService.getExternalId()))
                    .then()
                    .body("$", hasItem(allOf(hasEntry("email", userWithRoleViewInService1.getEmail()))));

            givenSetup()
                    .when()
                    .accept(JSON)
                    .header(HEADER_USER_CONTEXT, userWithRoleAdminInService1.getExternalId())
                    .delete(format("/v1/api/services/%s/users/%s", serviceExternalId, userWithRoleViewInService1.getExternalId()))
                    .then()
                    .statusCode(204)
                    .body(emptyString());

            givenSetup()
                    .when()
                    .accept(JSON)
                    .get(format("/v1/api/services/%s/users", serviceExternalId))
                    .then()
                    .body("$", not(hasItem(allOf(hasEntry("email", userWithRoleViewInService1.getEmail())))));

            givenSetup()
                    .when()
                    .accept(JSON)
                    .get(format("/v1/api/services/%s/users", anotherService.getExternalId()))
                    .then()
                    .body("$", hasItem(allOf(hasEntry("email", userWithRoleViewInService1.getEmail()))));
        }

        @Test
        void should_return_409_if_user_tries_to_delete_itself() {
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
        void should_return_403_if_GovUkPay_User_Context_Header_is_empty() {
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
        void should_return_403_if_GovUkPay_User_Context_Header_is_missing() {
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
