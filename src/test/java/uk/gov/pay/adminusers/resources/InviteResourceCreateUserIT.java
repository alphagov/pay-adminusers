package uk.gov.pay.adminusers.resources;

import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceName;
import uk.gov.pay.adminusers.model.User;

import java.util.Map;

import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.InviteDbFixture.inviteDbFixture;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

public class InviteResourceCreateUserIT extends IntegrationTest {
    private Service service;
    private String roleAdminName;
    private String senderExternalId;

    @Before
    public void givenAnExistingServiceAndARole() {

        service = serviceDbFixture(databaseHelper)
                .insertService();

        roleAdminName = roleDbFixture(databaseHelper)
                .insertAdmin().getName();

        String username = randomUuid();
        String email = username + "@example.com";
        senderExternalId = userDbFixture(databaseHelper)
                .withServiceRole(service.getId(), ADMIN.getId())
                .withUsername(username)
                .withEmail(email)
                .insertUser()
                .getExternalId();
    }

    @Test
    public void createInvitation_shouldSucceed_whenInvitingANewUser() throws Exception {

        String email = randomAlphanumeric(5) + "-invite@example.com";

        Map<Object, Object> invitationRequest = Map.of(
                "sender", senderExternalId,
                "email", email,
                "role_name", roleAdminName,
                "service_external_id", service.getExternalId());

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(INVITE_USER_RESOURCE_URL)
                .then()
                .statusCode(CREATED.getStatusCode())
                .body("email", is(email.toLowerCase()))
                .body("telephone_number", is(nullValue()))
                .body("_links", hasSize(1))
                .body("_links[0].href", matchesPattern("^https://selfservice.pymnt.localdomain/invites/[0-9a-z]{32}$"))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("invite"));
    }

    @Test
    public void createInvitation_shouldFail_whenAnInviteWithTheGivenEmailAlreadyExists() throws Exception {

        String existingUserEmail = randomAlphanumeric(5) + "-invite@example.com";

        String serviceExternalId = randomUuid();
        inviteDbFixture(databaseHelper)
                .withEmail(existingUserEmail)
                .withServiceExternalId(serviceExternalId)
                .insertInvite();

        Map<Object, Object> invitationRequest = Map.of(
                "sender", senderExternalId,
                "email", existingUserEmail,
                "role_name", roleAdminName,
                "service_external_id", serviceExternalId);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post(INVITE_USER_RESOURCE_URL)
                .then()
                .statusCode(CONFLICT.getStatusCode())
                .body("errors", hasSize(1))
                .body("errors", hasItems(format("invite with email [%s] already exists", existingUserEmail)));
    }

    @Test
    public void createInvitation_shouldFail_ifUserAlreadyBelongToService() throws Exception {

        String existingUserUsername = randomUuid();
        String existingUserEmail = existingUserUsername + "-invite@example.com";

        String serviceExternalId = randomUuid();
        Integer serviceId = randomInt();
        inviteDbFixture(databaseHelper)
                .withEmail(existingUserEmail)
                .withServiceExternalId(serviceExternalId)
                .withServiceId(serviceId)
                .insertInvite();
        User user = userDbFixture(databaseHelper)
                .withUsername(existingUserUsername)
                .withEmail(existingUserEmail)
                .withServiceRole(Service.from(serviceId, serviceExternalId, new ServiceName("service name")), 2)
                .insertUser();


        Map<Object, Object> invitationRequest = Map.of(
                "sender", senderExternalId,
                "email", existingUserEmail,
                "role_name", roleAdminName,
                "service_external_id", serviceExternalId);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post(INVITE_USER_RESOURCE_URL)
                .then()
                .statusCode(PRECONDITION_FAILED.getStatusCode())
                .body("errors", hasSize(1))
                .body("errors", hasItems(format("user [%s] already in service [%s]", user.getExternalId(), serviceExternalId)));
    }

    @Test
    public void createInvitation_shouldFail_whenServiceDoesNotExist() throws Exception {

        String nonExistentServiceId = "non existant service external id";
        Map<Object, Object> invitationRequest = Map.of(
                "sender", senderExternalId,
                "email", randomAlphanumeric(5) + "-invite@example.com",
                "role_name", roleAdminName,
                "service_external_id", nonExistentServiceId);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post(INVITE_USER_RESOURCE_URL)
                .then()
                .statusCode(NOT_FOUND.getStatusCode())
                .body(isEmptyString());
    }

    @Test
    public void createInvitation_shouldFail_whenRoleDoesNotExist() throws Exception {

        Map<Object, Object> invitationRequest = Map.of(
                "sender", senderExternalId,
                "email", randomAlphanumeric(5) + "-invite@example.com",
                "role_name", "non-existing-role",
                "service_external_id", service.getExternalId());

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post(INVITE_USER_RESOURCE_URL)
                .then()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("errors", hasSize(1))
                .body("errors", hasItems("role [non-existing-role] not recognised"));
    }

    @Test
    public void createInvitation_shouldFail_whenSenderDoesNotExist() throws Exception {

        String email = randomAlphanumeric(5) + "-invite@example.com";

        Map<Object, Object> invitationRequest = Map.of(
                "sender", "does-not-exist",
                "email", email,
                "role_name", roleAdminName,
                "service_external_id", service.getExternalId());

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(INVITE_USER_RESOURCE_URL)
                .then()
                .statusCode(FORBIDDEN.getStatusCode());
    }

    @Test
    public void createInvitation_shouldFail_whenSenderDoesNotHaveAdminRole() throws Exception {

        int otherRoleId = roleDbFixture(databaseHelper).insertRole().getId();
        String senderUsername = randomUuid();
        String senderEmail = senderUsername + "@example.com";
        String senderWithNoAdminRole = userDbFixture(databaseHelper)
                .withServiceRole(service.getId(), otherRoleId)
                .withUsername(senderUsername)
                .withEmail(senderEmail)
                .insertUser().getExternalId();

        Map<Object, Object> invitationRequest = Map.of(
                "sender", senderWithNoAdminRole,
                "email", randomUuid() + "-invite@example.com",
                "role_name", roleAdminName,
                "service_external_id", service.getExternalId());

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(INVITE_USER_RESOURCE_URL)
                .then()
                .statusCode(FORBIDDEN.getStatusCode());

    }

    @Test
    public void createInvitation_shouldFail_whenSenderDoesNotBelongToTheGivenService() throws Exception {

        Service otherService = serviceDbFixture(databaseHelper)
                .insertService();

        String senderUsername = randomUuid();
        String senderEmail = senderUsername + "@example.com";
        String senderExternalId = userDbFixture(databaseHelper)
                .withServiceRole(otherService.getId(), ADMIN.getId())
                .withUsername(senderUsername)
                .withEmail(senderEmail)
                .insertUser().getExternalId();

        Map<Object, Object> invitationRequest = Map.of(
                "sender", senderExternalId,
                "email", randomUuid() + "-invite@example.com",
                "role_name", roleAdminName,
                "service_external_id", service.getExternalId());

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(INVITE_USER_RESOURCE_URL)
                .then()
                .statusCode(FORBIDDEN.getStatusCode());
    }
}
