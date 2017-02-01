package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.model.User;

import java.util.Map;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ResetPasswordResourceTest extends IntegrationTest {

    private static final String RESET_PASSWORD_RESOURCE_URL = "/v1/api/reset-password";
    private static final String FORGOTTEN_PASSWORD_CODE = "mysuperduperresetcode";
    public static final String CURRENT_PASSWORD = "myOldEncryptedPassword";
    private ObjectMapper mapper;
    private static final int USER_ID = nextInt();

    @Before
    public void before() throws Exception {
        mapper = new ObjectMapper();
        databaseTestHelper.add(aUser(USER_ID, CURRENT_PASSWORD));
        databaseTestHelper.add(aForgottenPassword(FORGOTTEN_PASSWORD_CODE), USER_ID);
    }

    @Test
    public void resetPassword_shouldReturn204_whenCodeIsValid_changingTheOldPasswordToTheNewEncryptedOne() throws Exception {

        String password = "iPromiseIWon'tForgetThisPassword";
        ImmutableMap<Object, Object> payload = ImmutableMap.builder()
                .put("forgotten_password_code", FORGOTTEN_PASSWORD_CODE)
                .put("new_password", password)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .contentType(JSON)
                .accept(JSON)
                .post(RESET_PASSWORD_RESOURCE_URL)
                .then()
                .statusCode(204);

        Map<String, Object> userAttributes = databaseTestHelper.findUser(USER_ID).get(0);
        Object userPassword = userAttributes.get("password");

        assertThat(userPassword, is(notNullValue()));
        assertThat(userPassword, is(not(password)));
        assertThat(userPassword, is(not(CURRENT_PASSWORD)));
    }

    @Test
    public void resetPassword_shouldReturn400_whenCodeIsInvalid_andCurrentEncryptedPasswordShouldNotChange() throws Exception {

        ImmutableMap<Object, Object> payload = ImmutableMap.builder()
                .put("forgotten_password_code", "aCodeThatDoesNotExist")
                .put("new_password", "iPromiseIWon'tForgetThisPassword")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .contentType(JSON)
                .accept(JSON)
                .post(RESET_PASSWORD_RESOURCE_URL)
                .then()
                .statusCode(400);

        Map<String, Object> userAttributes = databaseTestHelper.findUser(USER_ID).get(0);
        Object userPassword = userAttributes.get("password");

        assertThat(userPassword, is(CURRENT_PASSWORD));
    }

    private ForgottenPassword aForgottenPassword(String random) {
        return ForgottenPassword.forgottenPassword(random, format("%s-name", random));
    }

    private User aUser(int id, String encryptedPassword) {
        return User.from(id, RandomStringUtils.randomAlphabetic(5), encryptedPassword, "user@email.com", "1", "784rh", "8948924");
    }
}
