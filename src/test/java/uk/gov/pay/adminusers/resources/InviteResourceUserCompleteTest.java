package uk.gov.pay.adminusers.resources;

import org.junit.Test;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.InviteDbFixture.inviteDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class InviteResourceUserCompleteTest extends IntegrationTest {
    public static final String INVITES_RESOURCE_URL = "/v1/api/invites";

    @Test
    public void shouldReturn200WithDisabledInvite_whenExistingUserSubscribingToAnExistingService() throws Exception {
        String email = format("%s@example.gov.uk", randomUuid());
        String telephoneNumber = "088882345689";
        String password = "valid_password";
        String serviceExternalId = randomUuid();
        String userExternalId = randomUuid();

        userDbFixture(databaseHelper)
                .withExternalId(userExternalId)
                .withEmail(email)
                .insertUser();

        String inviteCode = inviteDbFixture(databaseHelper)
                .withServiceExternalId(serviceExternalId)
                .withTelephoneNumber(telephoneNumber)
                .withEmail(email)
                .withPassword(password)
                .insertInvite();


        givenSetup()
                .when()
                .post(INVITES_RESOURCE_URL + "/" + inviteCode + "/complete")
                .then()
                .statusCode(OK.getStatusCode())
                .body("invite.disabled", is(true))
                .body("service_external_id", is(serviceExternalId))
                .body("user_external_id", is(userExternalId));

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .get(format(USER_RESOURCE_URL, userExternalId))
                .then()
                .statusCode(200)
                .body("service_roles", hasSize(1))
                .body("service_roles[0].service.external_id", is(serviceExternalId));
    }

    @Test
    public void shouldReturn404_whenInviteCodeNotFound() throws Exception {
        givenSetup()
                .when()
                .post(INVITES_RESOURCE_URL + "/" + "non-existent-code" + "/complete")
                .then()
                .statusCode(NOT_FOUND.getStatusCode());

    }

    @Test
    public void shouldReturn410_withDisabledInvite() throws Exception {
        String email = format("%s@example.gov.uk", randomUuid());
        String telephoneNumber = "088882345689";
        String password = "valid_password";
        String serviceExternalId = randomUuid();
        String userExternalId = randomUuid();

        userDbFixture(databaseHelper)
                .withExternalId(userExternalId)
                .withEmail(email)
                .insertUser();

        String inviteCode = inviteDbFixture(databaseHelper)
                .withServiceExternalId(serviceExternalId)
                .withTelephoneNumber(telephoneNumber)
                .withEmail(email)
                .withPassword(password)
                .insertInvite();


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
}
