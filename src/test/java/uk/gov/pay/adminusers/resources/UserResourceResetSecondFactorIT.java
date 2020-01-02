package uk.gov.pay.adminusers.resources;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.User;

import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class UserResourceResetSecondFactorIT extends IntegrationTest {

    private static final String OTP_KEY = "34f34";
    private String externalId;

    @Before
    public void createValidUser() {
        User user = userDbFixture(databaseHelper)
                .withSecondFactorMethod(SecondFactorMethod.APP)
                .withOtpKey(OTP_KEY)
                .insertUser();

        this.externalId = user.getExternalId();
    }

    @Test
    public void shouldResetSecondFactorMethod() {
        givenSetup()
                .when()
                .post("v1/api/users/" + externalId + "/reset-second-factor")
                .then()
                .statusCode(200)
                .body("second_factor", is("SMS"));
    }

    @Test
    public void shouldReturnNotFound_whenUserNotFound() {
        givenSetup()
                .when()
                .post("v1/api/users/not-found/reset-second-factor")
                .then()
                .statusCode(404);
    }
}
