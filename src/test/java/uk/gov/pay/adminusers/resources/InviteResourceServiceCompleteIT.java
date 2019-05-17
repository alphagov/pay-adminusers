package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import io.restassured.response.ValidatableResponse;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.InviteDbFixture.inviteDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class InviteResourceServiceCompleteIT extends IntegrationTest {
    public static final String INVITES_RESOURCE_URL = "/v1/api/invites";

    @Test
    public void shouldReturn200withDisabledInviteLinkingToCreatedUser_WhenPassedAValidInviteCode_withoutGatewayAccountIds() {
        String email = format("%s@example.gov.uk", randomUuid());
        String telephoneNumber = "+447700900000";
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
                .body("invite._links", hasSize(1))
                .body("invite._links[0].href", matchesPattern("^http://localhost:8080/v1/api/users/[0-9a-z]{32}$"))
                .body("invite._links[0].rel", is("user"))
                .body("invite.disabled", is(true))
                .body("user_external_id", matchesPattern("[0-9a-z]{32}$"))
                .body("service_external_id", matchesPattern("[0-9a-z]{32}$"));


        Map<String, Object> createdUser = databaseHelper.findUserByUsername(email).stream().findFirst().get();
        Map<String, Object> role = databaseHelper.findServiceRoleForUser((Integer) createdUser.get("id")).stream().findFirst().get();
        Map<String, Object> invite = databaseHelper.findInviteByCode(inviteCode).stream().findFirst().get();

        assertThat(role.get("id"), is(invite.get("role_id")));

        assertThat(createdUser.get("password"), is(password));
        assertThat(createdUser.get("email"), is(email));
        assertThat(createdUser.get("telephone_number"), is(telephoneNumber));
    }

    @Test
    public void shouldReturn200withDisabledInviteLinkingToCreatedUser_WhenPassedAValidInviteCode_withGatewayAccountIds() throws Exception {
        String email = format("%s@example.gov.uk", randomUuid());
        String telephoneNumber = "+447700900000";
        String password = "valid_password";
        String gatewayAccountId1 = String.valueOf(randomInt());
        String gatewayAccountId2 = String.valueOf(randomInt());

        ImmutableMap<String, List<String>> payload = ImmutableMap.of("gateway_account_ids", asList(gatewayAccountId1, gatewayAccountId2));

        String inviteCode = inviteDbFixture(databaseHelper)
                .withTelephoneNumber(telephoneNumber)
                .withEmail(email)
                .withPassword(password)
                .insertServiceInvite();
        ValidatableResponse validatableResponse = givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .post(INVITES_RESOURCE_URL + "/" + inviteCode + "/complete")
                .then()
                .statusCode(OK.getStatusCode());
        validatableResponse
                .body("invite._links", hasSize(1))
                .body("invite._links[0].href", matchesPattern("^http://localhost:8080/v1/api/users/[0-9a-z]{32}$"))
                .body("invite._links[0].rel", is("user"))
                .body("invite.disabled", is(true))
                .body("user_external_id", matchesPattern("[0-9a-z]{32}$"))
                .body("service_external_id", matchesPattern("[0-9a-z]{32}$"));

        String userExternalId = validatableResponse.extract().path("user_external_id");
        String serviceExternalId = validatableResponse.extract().path("service_external_id");

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .get(format(USER_RESOURCE_URL, userExternalId))
                .then()
                .statusCode(200)
                .body("external_id", is(userExternalId))
                .body("service_roles[0].service.external_id", is(serviceExternalId))
                .body("service_roles[0].service.gateway_account_ids", hasItems(gatewayAccountId1, gatewayAccountId2));
    }

    @Test
    public void shouldReturn410_WhenSameInviteCodeCompletedTwice() {
        String email = format("%s@example.gov.uk", randomUuid());
        String telephoneNumber = "+447700900000";
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
    public void shouldReturn409_ifAUserExistsWithTheSameEmail_whenServiceInviteCompletes() {
        String email = format("%s@example.gov.uk", randomUuid());
        String username = email;
        String telephoneNumber = "+447700900000";
        String password = "valid_password";

        String inviteCode = inviteDbFixture(databaseHelper)
                .withTelephoneNumber(telephoneNumber)
                .withEmail(email)
                .withPassword(password)
                .insertServiceInvite();

        userDbFixture(databaseHelper)
                .withUsername(username)
                .withEmail(email)
                .insertUser();

        givenSetup()
                .when()
                .post(INVITES_RESOURCE_URL + "/" + inviteCode + "/complete")
                .then()
                .statusCode(CONFLICT.getStatusCode());
    }

    @Test
    public void shouldReturn410_WheninviteIsDisabled() {
        String email = format("%s@example.gov.uk", randomUuid());
        String telephoneNumber = "+447700900000";
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
