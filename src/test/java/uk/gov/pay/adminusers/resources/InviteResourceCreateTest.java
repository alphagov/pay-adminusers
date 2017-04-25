package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Test;

import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

public class InviteResourceCreateTest extends IntegrationTest {

    private int serviceId;
    private String roleAdminName;
    private String senderExternalId;

    @Before
    public void givenAnExistingServiceAndARole() {

        serviceId = serviceDbFixture(databaseHelper)
                .insertService();

        roleAdminName = roleDbFixture(databaseHelper)
                .insertAdmin().getName();

        senderExternalId = userDbFixture(databaseHelper)
                .withServiceRole(serviceId, ADMIN.getId())
                .insertUser().getExternalId();
    }

    @Test
    public void createInvitation_shouldSucceed_whenInvitingANewUser() throws Exception {

        String email = randomAlphanumeric(5) + "-invite@example.com";

        ImmutableMap<Object, Object> invitationRequest = ImmutableMap.builder()
                .put("sender", senderExternalId)
                .put("email", email)
                .put("role_name", roleAdminName)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(format(SERVICE_INVITES_RESOURCE_URL, serviceId))
                .then()
                .statusCode(CREATED.getStatusCode())
                .body("email", is(email.toLowerCase()))
                .body("telephone_number", is(nullValue()))
                .body("_links", hasSize(1))
                .body("_links[0].href", matchesPattern("^http://selfservice/invites/[0-9a-z]{20,30}$"))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("invite"));
    }

    @Test
    public void createInvitation_shouldFail_whenEmailAlreadyExists() throws Exception {

        // This test will be removed when users can be added to existing services (multiple services supported) but
        // not at the moment.
        String existingUserEmail = randomAlphanumeric(5) + "-invite@example.com";

        userDbFixture(databaseHelper)
                .withEmail(existingUserEmail)
                .insertUser();

        ImmutableMap<Object, Object> invitationRequest = ImmutableMap.builder()
                .put("sender", senderExternalId)
                .put("email", existingUserEmail)
                .put("role_name", roleAdminName)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post(format(SERVICE_INVITES_RESOURCE_URL, serviceId))
                .then()
                .statusCode(409)
                .body("errors", hasSize(1))
                .body("errors", hasItems(format("email [%s] already exists", existingUserEmail)));
    }

    @Test
    public void createInvitation_shouldFail_whenServiceDoesNotExist() throws Exception {

        int nonExistentServiceId = 99999;
        ImmutableMap<Object, Object> invitationRequest = ImmutableMap.builder()
                .put("sender", senderExternalId)
                .put("email", randomAlphanumeric(5) + "-invite@example.com")
                .put("role_name", roleAdminName)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post(format(SERVICE_INVITES_RESOURCE_URL, nonExistentServiceId))
                .then()
                .statusCode(404)
                .body(isEmptyString());
    }

    @Test
    public void createInvitation_shouldFail_whenRoleDoesNotExist() throws Exception {

        ImmutableMap<Object, Object> invitationRequest = ImmutableMap.builder()
                .put("sender", senderExternalId)
                .put("email", randomAlphanumeric(5) + "-invite@example.com")
                .put("role_name", "non-existing-role")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post(format(SERVICE_INVITES_RESOURCE_URL, serviceId))
                .then()
                .statusCode(400)
                .body("errors", hasSize(1))
                .body("errors", hasItems("role [non-existing-role] not recognised"));
    }

    @Test
    public void createInvitation_shouldFail_whenSenderDoesNotExist() throws Exception {

        String email = randomAlphanumeric(5) + "-invite@example.com";

        ImmutableMap<Object, Object> invitationRequest = ImmutableMap.builder()
                .put("sender", "does-not-exist")
                .put("email", email)
                .put("role_name", roleAdminName)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(format(SERVICE_INVITES_RESOURCE_URL, serviceId))
                .then()
                .statusCode(FORBIDDEN.getStatusCode());
    }

    @Test
    public void createInvitation_shouldFail_whenSenderDoesNotHaveAdminRole() throws Exception {

        int otherRoleId = roleDbFixture(databaseHelper).insertRole().getId();
        String senderWithNoAdminRole = userDbFixture(databaseHelper)
                .withServiceRole(serviceId, otherRoleId)
                .insertUser().getExternalId();

        String email = randomAlphanumeric(5) + "-invite@example.com";

        ImmutableMap<Object, Object> invitationRequest = ImmutableMap.builder()
                .put("sender", senderWithNoAdminRole)
                .put("email", email)
                .put("role_name", roleAdminName)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(format(SERVICE_INVITES_RESOURCE_URL, serviceId))
                .then()
                .statusCode(FORBIDDEN.getStatusCode());

    }

    @Test
    public void createInvitation_shouldFail_whenSenderDoesNotBelongToTheGivenService() throws Exception {

        int otherServiceId = serviceDbFixture(databaseHelper)
                .insertService();

        String senderExternalId = userDbFixture(databaseHelper)
                .withServiceRole(otherServiceId, ADMIN.getId())
                .insertUser().getExternalId();

        String email = randomAlphanumeric(5) + "-invite@example.com";

        ImmutableMap<Object, Object> invitationRequest = ImmutableMap.builder()
                .put("sender", senderExternalId)
                .put("email", email)
                .put("role_name", roleAdminName)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(format(SERVICE_INVITES_RESOURCE_URL, serviceId))
                .then()
                .statusCode(FORBIDDEN.getStatusCode());
    }
}
