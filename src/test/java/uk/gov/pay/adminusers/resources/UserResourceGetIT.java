package uk.gov.pay.adminusers.resources;

import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;

import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

class UserResourceGetIT extends IntegrationTest {

    @Test
    void should_return_empty_map_when_getting_admin_emails_for_gateway_accounts() throws Exception {
        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .body(mapper.writeValueAsString(Map.of("gatewayAccountIds", List.of("gatewayAccount1"))))
                .post("/v1/api/users/admin-emails-for-gateway-accounts")
                .then()
                .statusCode(200)
                .body("gatewayAccount1", hasSize(0));
    }
    
    @Test
    void should_return_admin_emails_for_gateway_accounts() throws Exception {
        String gatewayAccount1 = valueOf(nextInt());
        String gatewayAccount2 = valueOf(nextInt());
        Service service = serviceDbFixture(databaseHelper).withGatewayAccountIds(gatewayAccount1, gatewayAccount2)
                .insertService();
        
        Role adminRole = roleDbFixture(databaseHelper).insertAdmin();
        var adminUser1 = userDbFixture(databaseHelper).withServiceRole(service.getId(), adminRole.getId()).insertUser();
        var adminUser2 = userDbFixture(databaseHelper).withServiceRole(service.getId(), adminRole.getId()).insertUser();
        
        Role viewOnlyRole = roleDbFixture(databaseHelper).withName("view-only").insertRole();
        userDbFixture(databaseHelper).withServiceRole(service.getId(), viewOnlyRole.getId()).insertUser();

        var gatewayAccountIds = Map.of("gatewayAccountIds", List.of(gatewayAccount1, gatewayAccount2));
        
        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .body(mapper.writeValueAsString(gatewayAccountIds))
                .post("/v1/api/users/admin-emails-for-gateway-accounts")
                .then()
                .statusCode(200)
                .body(gatewayAccount1, hasSize(2))
                .body(gatewayAccount1, hasItems(adminUser1.getEmail(), adminUser2.getEmail()))
                .body(gatewayAccount2, hasSize(2))
                .body(gatewayAccount2, hasItems(adminUser1.getEmail(), adminUser2.getEmail()));
    }
    
    @Test
    void shouldReturnUser_whenGetUserWithExternalId() {
        String gatewayAccount1 = valueOf(nextInt());
        String gatewayAccount2 = valueOf(nextInt());
        Service service = serviceDbFixture(databaseHelper).withGatewayAccountIds(gatewayAccount1, gatewayAccount2).insertService();
        String serviceExternalId = service.getExternalId();
        Role role = roleDbFixture(databaseHelper).insertRole();
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withServiceRole(service.getId(), role.getId()).withUsername(username).withEmail(email).insertUser();

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .get(format(USER_RESOURCE_URL, user.getExternalId()))
                .then()
                .statusCode(200)
                .body("external_id", is(user.getExternalId()))
                .body("username", is(user.getUsername()))
                .body("password", nullValue())
                .body("email", is(user.getEmail()))
                .body("service_roles", hasSize(1))
                .body("service_roles[0].service.external_id", is(serviceExternalId))
                .body("service_roles[0].service.name", is(service.getName()))
                .body("telephone_number", is(user.getTelephoneNumber()))
                .body("otp_key", is(user.getOtpKey()))
                .body("login_counter", is(0))
                .body("disabled", is(false))
                .body("service_roles[0].role.name", is(role.getName()))
                .body("service_roles[0].role.description", is(role.getDescription()))
                .body("service_roles[0].role.permissions", hasSize(role.getPermissions().size()))
                .body("_links", hasSize(1))
                .body("_links[0].href", is("http://localhost:8080/v1/api/users/" + user.getExternalId()))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("self"));
    }


    @Test
    void shouldReturn404_whenGetUser_withNonExistentExternalId() {
        givenSetup()
                .when()
                .accept(JSON)
                .get(format(USER_RESOURCE_URL, "non-existent-user"))
                .then()
                .statusCode(404);
    }

    @Test
    void shouldReturn404_whenGetUser_withInvalidMaxLengthExternalId() {
        givenSetup()
                .when()
                .accept(JSON)
                .get(format(USER_RESOURCE_URL, randomAlphanumeric(256)))
                .then()
                .statusCode(404);
    }
}

