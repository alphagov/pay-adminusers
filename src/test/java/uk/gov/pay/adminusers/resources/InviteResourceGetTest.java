package uk.gov.pay.adminusers.resources;

import org.junit.Test;

import static com.jayway.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.InviteDbFixture.inviteDbFixture;

public class InviteResourceGetTest extends IntegrationTest {

    @Test
    public void getInvitation_shouldSucceed() throws Exception {

        String email = "user@example.com";
        String inviteCode = inviteDbFixture(databaseHelper)
                .withEmail(email)
                .insertInvite();

        givenSetup()
                .when()
                .accept(JSON)
                .get(INVITES_RESOURCE_URL + "/" + inviteCode)
                .then()
                .statusCode(200)
                .body("email", is(email))
                .body("telephone_number", is(nullValue()));
    }

    @Test
    /**
     *  This situation happens when OTP is generated (with telephone_number) and then the GET Invite is again requested
     *  (still non-expired invite link)
     */
    public void getInvitation_shouldSucceedWithTelephoneNumber_whenIsAvailable() throws Exception {

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
                .statusCode(200)
                .body("email", is(email))
                .body("telephone_number", is(telephoneNumber));
    }

    @Test
    public void getInvitation_shouldFail_whenExpired() throws Exception {

        String expiredCode = inviteDbFixture(databaseHelper).expired().insertInvite();

        givenSetup()
                .when()
                .accept(JSON)
                .get(INVITES_RESOURCE_URL + "/" + expiredCode)
                .then()
                .statusCode(410);
    }

    @Test
    public void createInvitation_shouldFail_whenInvalidCode() throws Exception {

        givenSetup()
                .when()
                .accept(JSON)
                .get(INVITES_RESOURCE_URL + "/fake-code")
                .then()
                .statusCode(404);
    }
}
