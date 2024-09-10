package uk.gov.pay.adminusers.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;

import java.util.Map;

import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.InviteDbFixture.inviteDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

class InviteResourceServiceCompleteIT extends IntegrationTest {

    private Role adminRole;

    @BeforeEach
    public void setUp() {
        adminRole = getInjector().getInstance(RoleDao.class).findByRoleName(RoleName.ADMIN).get().toRole();
    }
    
    @Test
    void shouldReturn200withDisabledInviteLinkingToCreatedUser_WhenPassedAValidInviteCode() throws Exception {
        String email = format("%s@example.gov.uk", randomUuid());
        String telephoneNumber = "+447700900000";
        String password = "valid_password";

        String inviteCode = inviteDbFixture(databaseHelper)
                .withTelephoneNumber(telephoneNumber)
                .withEmail(email)
                .withPassword(password)
                .insertSelfSignupInvite(adminRole);

        Map<String, String> payload = Map.of("second_factor", "SMS");

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .post(INVITES_RESOURCE_URL + "/" + inviteCode + "/complete")
                .then()
                .statusCode(OK.getStatusCode())
                .body("invite._links", hasSize(1))
                .body("invite._links[0].href", matchesPattern("^http://localhost:8080/v1/api/users/[0-9a-z]{32}$"))
                .body("invite._links[0].rel", is("user"))
                .body("invite.disabled", is(true))
                .body("user_external_id", matchesPattern("[0-9a-z]{32}$"))
                .body("$", not(hasKey("service_external_id")));


        Map<String, Object> createdUser = databaseHelper.findUserByEmail(email).stream().findFirst().get();
        Map<String, Object> invite = databaseHelper.findInviteByCode(inviteCode).stream().findFirst().get();

        assertThat(createdUser.get("password"), is(password));
        assertThat(createdUser.get("email"), is(email));
        assertThat(createdUser.get("telephone_number"), is(telephoneNumber));
        
        assertThat(invite.get("disabled"), is(true));
    }

    @Test
    void shouldReturn400_whenUnsupportedSecondFactorMethodSupplied() throws Exception {
        String inviteCode = "an invite code";

        Map<String, String> payload = Map.of("second_factor", "BAD_VALUE");

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .post(INVITES_RESOURCE_URL + "/" + inviteCode + "/complete")
                .then()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body(containsString("BAD_VALUE"));
    }

    @Test
    void shouldReturn410_WhenSameInviteCodeCompletedTwice() throws Exception {
        String email = format("%s@example.gov.uk", randomUuid());
        String telephoneNumber = "+447700900000";
        String password = "valid_password";

        String inviteCode = inviteDbFixture(databaseHelper)
                .withTelephoneNumber(telephoneNumber)
                .withEmail(email)
                .withPassword(password)
                .insertSelfSignupInvite(adminRole);

        Map<String, String> payload = Map.of("second_factor", "SMS");

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .post(INVITES_RESOURCE_URL + "/" + inviteCode + "/complete")
                .then()
                .statusCode(OK.getStatusCode());

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .post(INVITES_RESOURCE_URL + "/" + inviteCode + "/complete")
                .then()
                .statusCode(GONE.getStatusCode());
    }

    @Test
    void shouldReturn409_ifAUserExistsWithTheSameEmail_whenServiceInviteCompletes() throws Exception {
        String email = format("%s@example.gov.uk", randomUuid());
        String username = email;
        String telephoneNumber = "+447700900000";
        String password = "valid_password";

        String inviteCode = inviteDbFixture(databaseHelper)
                .withTelephoneNumber(telephoneNumber)
                .withEmail(email)
                .withPassword(password)
                .insertSelfSignupInvite(adminRole);

        Map<String, String> payload = Map.of("second_factor", "SMS");

        userDbFixture(databaseHelper)
                .withEmail(email)
                .insertUser();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .post(INVITES_RESOURCE_URL + "/" + inviteCode + "/complete")
                .then()
                .statusCode(CONFLICT.getStatusCode());
    }

    @Test
    void shouldReturn410_WhenInviteIsDisabled() throws Exception {
        String email = format("%s@example.gov.uk", randomUuid());
        String telephoneNumber = "+447700900000";
        String password = "valid_password";

        String inviteCode = inviteDbFixture(databaseHelper)
                .withTelephoneNumber(telephoneNumber)
                .withEmail(email)
                .withPassword(password)
                .disabled()
                .insertSelfSignupInvite(adminRole);

        Map<String, String> payload = Map.of("second_factor", "SMS");

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .post(INVITES_RESOURCE_URL + "/" + inviteCode + "/complete")
                .then()
                .statusCode(GONE.getStatusCode());
    }

}
