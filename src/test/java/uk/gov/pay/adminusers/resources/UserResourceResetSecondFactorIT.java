package uk.gov.pay.adminusers.resources;

import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.User;

import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class UserResourceResetSecondFactorIT extends IntegrationTest {

    private static final String OTP_KEY = "34f34";

    @Test
    public void shouldResetSecondFactorMethod() {
        User user = userDbFixture(databaseHelper)
                .withSecondFactorMethod(SecondFactorMethod.APP)
                .withOtpKey(OTP_KEY)
                .insertUser();
        
        givenSetup()
                .when()
                .post("v1/api/users/" + user.getExternalId() + "/reset-second-factor")
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

    @Test
    public void shouldReturnPreconditionFailed_whenUserDoesNotHaveTelephoneNumber() {
        User user = userDbFixture(databaseHelper)
                .withSecondFactorMethod(SecondFactorMethod.APP)
                .withOtpKey(OTP_KEY)
                .withTelephoneNumber(null)
                .insertUser();
        
        givenSetup()
                .when()
                .post("v1/api/users/" + user.getExternalId() + "/reset-second-factor")
                .then()
                .statusCode(412);
    }
}
