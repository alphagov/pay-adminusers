package uk.gov.pay.adminusers.resources;

import org.junit.Test;

import static com.jayway.restassured.http.ContentType.JSON;
import static uk.gov.pay.adminusers.fixtures.InviteDbFixture.inviteDbFixture;

public class InviteResourceGetTest extends IntegrationTest {

    @Test
    public void getInvitation_shouldSucceed() throws Exception {

        String inviteCode = inviteDbFixture(databaseHelper).insertInvite();

        givenSetup()
                .when()
                .accept(JSON)
                .get(INVITES_RESOURCE_URL + "/" + inviteCode)
                .then()
                .statusCode(200);
    }

    @Test
    public void getInvitation_shouldFail_whenExpired() throws Exception {

        String expiredCode = inviteDbFixture(databaseHelper).expired().insertInvite();

        givenSetup()
                .when()
                .accept(JSON)
                .get(INVITES_RESOURCE_URL + "/" + expiredCode)
                .then()
                .statusCode(400);
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
