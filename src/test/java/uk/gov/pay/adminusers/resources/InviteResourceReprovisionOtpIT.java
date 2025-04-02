package uk.gov.pay.adminusers.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.fixtures.InviteDbFixture;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class InviteResourceReprovisionOtpIT extends IntegrationTest {

    private static final String OTP_KEY = "KPWXGUTNWOE7PMVK";
    private static final String EMAIL = "invited-" + random(5) + "@example.com";
    
    private Role adminRole;
    private String code;

    @BeforeEach
    void givenAnExistingInvite() {
        adminRole = getInjector().getInstance(RoleDao.class).findByRoleName(RoleName.ADMIN).get().toRole();
        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withEmail(EMAIL)
                .withOtpKey(OTP_KEY)
                .expired()
                .insertInviteToAddUserToService(adminRole);
    }

    @Test
    void reprovisionOtp_shouldFail_whenInviteCodeMalformatted() {
        givenSetup()
                .when()
                .contentType(JSON)
                .post(format(INVITES_REPROVISION_OTP_RESOURCE_URL, ""))
                .then()
                .statusCode(NOT_FOUND.getStatusCode());
    }
    
    @Test
    void reprovisionOtp_shouldReprovisionOtp_whenValidInviteCode() {
        givenSetup()
                .when()
                .contentType(JSON)
                .post(format(INVITES_REPROVISION_OTP_RESOURCE_URL, code))
                .then()
                .statusCode(OK.getStatusCode())
                .body("otp_key", not(nullValue()))
                .body("otp_key", not(OTP_KEY));

        Map<String, Object> foundInvite = databaseHelper.findInviteByCode(code).get();
        assertThat(foundInvite.get("otp_key"), not(nullValue()));
        assertThat(foundInvite.get("otp_key"), not(OTP_KEY));
    }
    
}
