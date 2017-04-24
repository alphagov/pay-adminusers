package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.http.ContentType;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;

import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class InviteResourceCreateTest extends IntegrationTest {

    private int serviceId;
    private String roleName;

    @Before
    public void givenAnExistingServiceAndARole() {

        serviceId = RandomUtils.nextInt();
        roleName = randomAlphabetic(5);

        serviceDbFixture(databaseHelper)
                .withId(serviceId)
                .insertService();

        roleDbFixture(databaseHelper)
                .withName(roleName)
                .insertRole();
    }

    @Test
    public void createInvitation_shouldSucceed_whenInvitingANewUser() throws Exception {

        ImmutableMap<Object, Object> invitationRequest = ImmutableMap.builder()
                .put("email", randomAlphanumeric(5) + "-invite@example.com")
                .put("role_name", roleName)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(format(SERVICE_INVITES_RESOURCE_URL, serviceId))
                .then()
                .statusCode(ACCEPTED.getStatusCode())
                .body(isEmptyString());
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
                .put("email", existingUserEmail)
                .put("role_name", roleName)
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
                .put("email", randomAlphanumeric(5) + "-invite@example.com")
                .put("role_name", roleName)
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
}
