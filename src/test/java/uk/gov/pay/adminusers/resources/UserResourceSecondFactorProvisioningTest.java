package uk.gov.pay.adminusers.resources;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.User;

import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class UserResourceSecondFactorProvisioningTest extends IntegrationTest {

    private static final String USER_2FA_PROVISION_URL = USER_2FA_URL + "/provision";
    private static final String ORIGINAL_OTP_KEY = "1111111111111111";

    private String externalId;
    private String username;

    @Before
    public void createValidUser() {
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withOtpKey(ORIGINAL_OTP_KEY).withUsername(username).withEmail(email).insertUser();

        this.externalId = user.getExternalId();
        this.username = user.getUsername();
    }

    @Test
    public void shouldProvisionNewOtpKey() {
        givenSetup()
                .when()
                .post(format(USER_2FA_PROVISION_URL, externalId))
                .then()
                .statusCode(200)
                .body("username", is(username))
                .body("otp_key", is(ORIGINAL_OTP_KEY))
                .body("provisional_otp_key", is(notNullValue()))
                .body("provisional_otp_key_created_at", is(notNullValue()));
    }

    @Test
    public void shouldReturnNotFoundIfUserNotFoundWhenProvisionNewOtpKey() {
        givenSetup()
                .when()
                .post(format(USER_2FA_PROVISION_URL, "this is not a valid user external ID"))
                .then()
                .statusCode(404);
    }

}
