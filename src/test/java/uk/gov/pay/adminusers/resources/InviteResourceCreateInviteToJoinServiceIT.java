package uk.gov.pay.adminusers.resources;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceName;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;

import java.util.Locale;
import java.util.Map;

import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.InviteDbFixture.inviteDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

class InviteResourceCreateInviteToJoinServiceIT extends IntegrationTest {

    private static final String CREATE_INVITE_TO_JOIN_SERVICE_RESOURCE_URL = "/v1/api/invites/create-invite-to-join-service";
    
    private Service service;
    private Role adminRole;
    private String senderExternalId;
    private RoleDao roleDao;
    
    @BeforeEach
    void givenAnExistingServiceAndARole() {

        service = serviceDbFixture(databaseHelper).insertService();
        
        roleDao = getInjector().getInstance(RoleDao.class);
        adminRole = roleDao.findByRoleName(RoleName.ADMIN).get().toRole();

        String username = randomUuid();
        String email = username + "@example.com";
        senderExternalId = userDbFixture(databaseHelper)
                .withServiceRole(service.getId(), adminRole)
                .withEmail(email)
                .insertUser()
                .getExternalId();
    }

    @Test
    void createInvitation_shouldSucceed_whenInvitingANewUser() throws Exception {

        String email = randomAlphanumeric(5) + "-invite@example.com";

        Map<Object, Object> invitationRequest = Map.of(
                "sender", senderExternalId,
                "email", email,
                "role_name", adminRole.getRoleName().getName(),
                "service_external_id", service.getExternalId());

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(CREATE_INVITE_TO_JOIN_SERVICE_RESOURCE_URL)
                .then()
                .statusCode(CREATED.getStatusCode())
                .body("email", is(email.toLowerCase(Locale.ENGLISH)))
                .body("telephone_number", is(nullValue()))
                .body("is_invite_to_join_service", is(true))
                .body("_links", hasSize(1))
                .body("_links[0].href", matchesPattern("^https://selfservice.pymnt.localdomain/invites/[0-9a-z]{32}$"))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("invite"));
    }

    @Test
    void createInvitation_shouldFail_whenAnInviteWithTheGivenEmailAlreadyExists() throws Exception {

        String existingUserEmail = randomAlphanumeric(5) + "-invite@example.com";

        String serviceExternalId = randomUuid();
        inviteDbFixture(databaseHelper)
                .withEmail(existingUserEmail)
                .withServiceExternalId(serviceExternalId)
                .insertInviteToAddUserToService(adminRole);

        Map<Object, Object> invitationRequest = Map.of(
                "sender", senderExternalId,
                "email", existingUserEmail,
                "role_name", adminRole.getRoleName().getName(),
                "service_external_id", serviceExternalId);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post(CREATE_INVITE_TO_JOIN_SERVICE_RESOURCE_URL)
                .then()
                .statusCode(CONFLICT.getStatusCode())
                .body("errors", hasSize(1))
                .body("errors", hasItems(format("invite with email [%s] already exists", existingUserEmail)));
    }

    @Test
    void createInvitation_shouldFail_ifUserAlreadyBelongToService() throws Exception {

        String existingUserUsername = randomUuid();
        String existingUserEmail = existingUserUsername + "-invite@example.com";

        String serviceExternalId = randomUuid();
        Integer serviceId = randomInt();
        inviteDbFixture(databaseHelper)
                .withEmail(existingUserEmail)
                .withServiceExternalId(serviceExternalId)
                .withServiceId(serviceId)
                .insertInviteToAddUserToService(adminRole);
        User user = userDbFixture(databaseHelper)
                .withEmail(existingUserEmail)
                .withServiceRole(Service.from(serviceId, serviceExternalId, new ServiceName("service name")), adminRole)
                .insertUser();

        Map<Object, Object> invitationRequest = Map.of(
                "sender", senderExternalId,
                "email", existingUserEmail,
                "role_name", adminRole.getRoleName().getName(),
                "service_external_id", serviceExternalId);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post(CREATE_INVITE_TO_JOIN_SERVICE_RESOURCE_URL)
                .then()
                .statusCode(PRECONDITION_FAILED.getStatusCode())
                .body("errors", hasSize(1))
                .body("errors", hasItems(format("user [%s] already in service [%s]", user.getExternalId(), serviceExternalId)));
    }

    @Test
    void createInvitation_shouldFail_whenServiceDoesNotExist() throws Exception {

        String nonExistentServiceId = "non existant service external id";
        Map<Object, Object> invitationRequest = Map.of(
                "sender", senderExternalId,
                "email", randomAlphanumeric(5) + "-invite@example.com",
                "role_name", adminRole.getRoleName().getName(),
                "service_external_id", nonExistentServiceId);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post(CREATE_INVITE_TO_JOIN_SERVICE_RESOURCE_URL)
                .then()
                .statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    void createInvitation_shouldFail_whenRoleDoesNotExist() throws Exception {

        Map<String, Object> invitationRequest = Map.of(
                "sender", senderExternalId,
                "email", randomAlphanumeric(5) + "-invite@example.com",
                "role_name", "non-existing-role",
                "service_external_id", service.getExternalId());

        givenSetup()
                .when()
                .body(invitationRequest)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post(CREATE_INVITE_TO_JOIN_SERVICE_RESOURCE_URL)
                .then()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", is("Unable to process JSON"))
                .body("details", containsString("\"non-existing-role\": not one of the values accepted for Enum class: [view-and-initiate-moto, view-only, view-and-refund, super-admin, view-refund-and-initiate-moto, admin]"));
    }

    @Test
    void createInvitation_shouldFail_whenSenderDoesNotExist() throws Exception {

        String email = randomAlphanumeric(5) + "-invite@example.com";

        Map<Object, Object> invitationRequest = Map.of(
                "sender", "does-not-exist",
                "email", email,
                "role_name", adminRole.getRoleName().getName(),
                "service_external_id", service.getExternalId());

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(CREATE_INVITE_TO_JOIN_SERVICE_RESOURCE_URL)
                .then()
                .statusCode(FORBIDDEN.getStatusCode());
    }

    @Test
    void createInvitation_shouldFail_whenSenderDoesNotHaveAdminRole() throws Exception {

        Role viewOnlyRole = roleDao.findByRoleName(RoleName.VIEW_ONLY).get().toRole();
        String senderUsername = randomUuid();
        String senderEmail = senderUsername + "@example.com";
        String senderWithNoAdminRole = userDbFixture(databaseHelper)
                .withServiceRole(service.getId(), viewOnlyRole)
                .withEmail(senderEmail)
                .insertUser().getExternalId();

        Map<Object, Object> invitationRequest = Map.of(
                "sender", senderWithNoAdminRole,
                "email", randomUuid() + "-invite@example.com",
                "role_name", adminRole.getRoleName().getName(),
                "service_external_id", service.getExternalId());

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(CREATE_INVITE_TO_JOIN_SERVICE_RESOURCE_URL)
                .then()
                .statusCode(FORBIDDEN.getStatusCode());

    }

    @Test
    void createInvitation_shouldFail_whenSenderDoesNotBelongToTheGivenService() throws Exception {

        Service otherService = serviceDbFixture(databaseHelper).insertService();

        String senderUsername = randomUuid();
        String senderEmail = senderUsername + "@example.com";
        String senderExternalId = userDbFixture(databaseHelper)
                .withServiceRole(otherService.getId(), adminRole)
                .withEmail(senderEmail)
                .insertUser().getExternalId();

        Map<Object, Object> invitationRequest = Map.of(
                "sender", senderExternalId,
                "email", randomUuid() + "-invite@example.com",
                "role_name", adminRole.getRoleName().getName(),
                "service_external_id", service.getExternalId());

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(CREATE_INVITE_TO_JOIN_SERVICE_RESOURCE_URL)
                .then()
                .statusCode(FORBIDDEN.getStatusCode());
    }

    @Test
    void createInvitation_shouldFail_whenMissingMandatoryFields() throws Exception {
        Map<Object, Object> invitationRequest = Map.of();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(CREATE_INVITE_TO_JOIN_SERVICE_RESOURCE_URL)
                .then()
                .statusCode(422)
                .body("errors", hasSize(4))
                .body("errors", hasItems(
                        "serviceExternalId must not be empty",
                        "roleName must not be null",
                        "sender must not be empty",
                        "email must not be empty"
                ));
    }

    @Test
    void createInvitation_shouldFail_whenInvalidEmail() throws Exception {
        Map<Object, Object> invitationRequest = Map.of(
                "sender", senderExternalId,
                "email", "invalid",
                "role_name", adminRole.getRoleName().getName(),
                "service_external_id", service.getExternalId());
        
        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(CREATE_INVITE_TO_JOIN_SERVICE_RESOURCE_URL)
                .then()
                .statusCode(422)
                .body("errors", hasSize(1))
                .body("errors", hasItems("email must be a well-formed email address"));
    }
}
