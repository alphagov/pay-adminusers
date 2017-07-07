package uk.gov.pay.adminusers.resources;

import org.junit.Test;

import java.util.Map;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static uk.gov.pay.adminusers.fixtures.InviteDbFixture.inviteDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class InviteResourceCompleteTest extends IntegrationTest {
    public static final String INVITES_RESOURCE_URL = "/v1/api/invites";

    @Test
    public void shouldReturn200withDisabledInviteLinkingToCreatedUser_WhenPassedAValidInviteCode() {
        String email = "example1@example.gov.uk";
        String telephoneNumber = "088882345689";
        String password = "valid_password";

        String inviteCode = inviteDbFixture(databaseHelper)
                .withTelephoneNumber(telephoneNumber)
                .withEmail(email)
                .withPassword(password)
                .insertServiceInvite();

        givenSetup()
                .when()
                .post(INVITES_RESOURCE_URL + "/" + inviteCode + "/complete")
                .then()
                .statusCode(OK.getStatusCode())
                .body("_links", hasSize(1))
                .body("_links[0].href", matchesPattern("^http://localhost:8080/v1/api/users/[0-9a-z]{32}$"))
                .body("_links[0].rel", is("user"))
                .body("disabled", is(true));

        Map<String, Object> createdUser = databaseHelper.findUserByUsername(email).stream().findFirst().get();
        Map<String, Object> role = databaseHelper.findServiceRoleForUser((Integer) createdUser.get("id")).stream().findFirst().get();
        Map<String, Object> invite = databaseHelper.findInviteByCode(inviteCode).stream().findFirst().get();

        assertThat(role.get("id"), is(invite.get("role_id")));
        assertThat(role.get("service_id"), is(invite.get("service_id")));

        assertThat(createdUser.get("password"), is(password));
        assertThat(createdUser.get("email"), is(email));
        assertThat(createdUser.get("telephone_number"), is(telephoneNumber));
    }

    @Test
    public void shouldReturn410_WhenSameInviteCodeCompletedTwice() {
        String email = "example2@example.gov.uk";
        String telephoneNumber = "088882345689";
        String password = "valid_password";

        String inviteCode = inviteDbFixture(databaseHelper)
                .withTelephoneNumber(telephoneNumber)
                .withEmail(email)
                .withPassword(password)
                .insertServiceInvite();

        givenSetup()
                .when()
                .post(INVITES_RESOURCE_URL + "/" + inviteCode + "/complete")
                .then()
                .statusCode(OK.getStatusCode());

        givenSetup()
                .when()
                .post(INVITES_RESOURCE_URL + "/" + inviteCode + "/complete")
                .then()
                .statusCode(GONE.getStatusCode());
    }

    @Test
    public void shouldReturn409_WhenUserWithSameEmailExists() {
        String email = "example3@example.gov.uk";
        String telephoneNumber = "088882345689";
        String password = "valid_password";


        userDbFixture(databaseHelper)
                .withTelephoneNumber(telephoneNumber)
                .withEmail(email)
                .withPassword(password)
                .insertUser();

        String inviteCode = inviteDbFixture(databaseHelper)
                .withTelephoneNumber(telephoneNumber)
                .withEmail(email)
                .withPassword(password)
                .insertServiceInvite();

        givenSetup()
                .when()
                .post(INVITES_RESOURCE_URL + "/" + inviteCode + "/complete")
                .then()
                .statusCode(CONFLICT.getStatusCode());
    }

    @Test
    public void shouldReturn410_WheninviteIsDisabled() {
        String email = "example4@example.gov.uk";
        String telephoneNumber = "088882345689";
        String password = "valid_password";

        String inviteCode = inviteDbFixture(databaseHelper)
                .withTelephoneNumber(telephoneNumber)
                .withEmail(email)
                .withPassword(password)
                .disabled()
                .insertServiceInvite();

        givenSetup()
                .when()
                .post(INVITES_RESOURCE_URL + "/" + inviteCode + "/complete")
                .then()
                .statusCode(GONE.getStatusCode());
    }
}
