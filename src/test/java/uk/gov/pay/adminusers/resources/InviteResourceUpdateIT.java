package uk.gov.pay.adminusers.resources;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.pay.adminusers.fixtures.InviteDbFixture.inviteDbFixture;

class InviteResourceUpdateIT extends IntegrationTest {

    @Test
    void validUpdateInviteRequest_shouldSucceed() throws Exception {
        String inviteCode = inviteDbFixture(databaseHelper).insertInviteToAddUserToService();

        String newPassword = "a-new-password";
        String newPhoneNumber = "+441134960000";
        List<Map<String, String>> request = List.of(
                Map.of("op", "replace",
                        "path", "password",
                        "value", newPassword),
                Map.of("op", "replace",
                        "path", "telephone_number",
                        "value", newPhoneNumber)
                );

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(request))
                .patch("/v1/api/invites/" + inviteCode)
                .then()
                .statusCode(OK.getStatusCode())
                .body("telephone_number", is(newPhoneNumber))
                .body("password_set", is(true));

        Map<String, Object> invite = databaseHelper.findInviteByCode(inviteCode).get();
        assertThat(invite.get("telephone_number"), is(newPhoneNumber));
        assertThat(invite.get("password"), not(nullValue()));
    }

    @Test
    void invalidUpdateInviteRequest_shouldReturn400() throws Exception {
        List<Map<String, Object>> request = List.of(
                Map.of("op", "replace",
                        "path", "telephone_number",
                        "value", 123)
        );

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(request))
                .patch("/v1/api/invites/valid-invite-code")
                .then()
                .statusCode(400)
                .body("errors", hasSize(1))
                .body("errors", hasItem("Value for path [telephone_number] must be a string"));
    }
}
