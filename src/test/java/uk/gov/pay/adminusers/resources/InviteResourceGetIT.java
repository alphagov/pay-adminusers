package uk.gov.pay.adminusers.resources;

import org.junit.Test;

import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.InviteDbFixture.inviteDbFixture;

public class InviteResourceGetIT extends IntegrationTest {

    @Test
    public void getInvitation_shouldSucceed() {

        String email = "user@example.com";
        String inviteCode = inviteDbFixture(databaseHelper)
                .withEmail(email)
                .insertInvite();

        givenSetup()
                .when()
                .accept(JSON)
                .get(INVITES_RESOURCE_URL + "/" + inviteCode)
                .then()
                .statusCode(OK.getStatusCode())
                .body("email", is(email))
                .body("telephone_number", is(nullValue()))
                .body("disabled", is(false))
                .body("user_exist", is(false))
                .body("attempt_counter", is(0));
    }

    @Test
    /**
     *  This situation happens when OTP is generated (with telephone_number) and then the GET Invite is again requested
     *  (still non-expired invite link)
     */
    public void getInvitation_shouldSucceedWithTelephoneNumber_whenIsAvailable() {

        String email = "user@example.com";
        String telephoneNumber = "+440787654534";

        String inviteCode = inviteDbFixture(databaseHelper)
                .withTelephoneNumber(telephoneNumber)
                .withEmail(email)
                .insertInvite();

        givenSetup()
                .when()
                .accept(JSON)
                .get(INVITES_RESOURCE_URL + "/" + inviteCode)
                .then()
                .statusCode(OK.getStatusCode())
                .body("email", is(email))
                .body("telephone_number", is(telephoneNumber))
                .body("disabled", is(false))
                .body("attempt_counter", is(0));
    }

    @Test
    public void getInvitation_shouldFail_whenExpired() {

        String expiredCode = inviteDbFixture(databaseHelper).expired().insertInvite();

        givenSetup()
                .when()
                .accept(JSON)
                .get(INVITES_RESOURCE_URL + "/" + expiredCode)
                .then()
                .statusCode(GONE.getStatusCode());
    }

    @Test
    public void getInvitation_shouldFail_whenDisabled() {

        String expiredCode = inviteDbFixture(databaseHelper).disabled().insertInvite();

        givenSetup()
                .when()
                .accept(JSON)
                .get(INVITES_RESOURCE_URL + "/" + expiredCode)
                .then()
                .statusCode(GONE.getStatusCode());
    }

    @Test
    public void createInvitation_shouldFail_whenInvalidCode() {

        givenSetup()
                .when()
                .accept(JSON)
                .get(INVITES_RESOURCE_URL + "/fake-code")
                .then()
                .statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    public void getInvitations_shouldSucceed() {
        String serviceExternalId = "sdfgsdgytgkh";
        String email = "user@example.com";
        inviteDbFixture(databaseHelper)
                .withEmail(email)
                .withServiceExternalId(serviceExternalId)
                .insertInvite();
        givenSetup()
                .when()
                .accept(JSON)
                .get(INVITES_RESOURCE_URL + "?serviceId=" + serviceExternalId)
                .then()
                .statusCode(OK.getStatusCode())
                .body("[0].email", is(email))
                .body("[0].telephone_number", is(nullValue()))
                .body("[0].disabled", is(false))
                .body("[0].expired", is(false))
                .body("[0].user_exist", is(false))
                .body("[0].attempt_counter", is(0));
    }
}
