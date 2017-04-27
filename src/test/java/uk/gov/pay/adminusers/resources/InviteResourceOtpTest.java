package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.fixtures.InviteDbFixture;

import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

public class InviteResourceOtpTest extends IntegrationTest {

    private String code;

    @Before
    public void givenAnExistingInvite() {
        code = InviteDbFixture.inviteDbFixture(databaseHelper).expired().insertInvite();
    }

    @Test
    public void generateOtp_shouldSucceed_evenWhenTokenIsExpired_sinceItShouldBeValidatedOnGetInvite() throws Exception {

        String telephoneNumber = "+447999999999";

        ImmutableMap<Object, Object> invitationRequest = ImmutableMap.builder()
                .put("telephone_number", telephoneNumber)
                .put("password", "a-secure-password")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(String.format(INVITES_OTP_RESOURCE_URL, code))
                .then()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void generateOtp_shouldFail_whenInviteDoesNotExist() throws Exception {

        String telephoneNumber = "+447999999999";

        ImmutableMap<Object, Object> invitationRequest = ImmutableMap.builder()
                .put("telephone_number", telephoneNumber)
                .put("password", "a-secure-password")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(format(INVITES_OTP_RESOURCE_URL, "not-existing-code"))
                .then()
                .statusCode(NOT_FOUND.getStatusCode());
    }
}
