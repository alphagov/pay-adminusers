package uk.gov.pay.adminusers.resources;

import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.fixtures.ForgottenPasswordDbFixture;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.ForgottenPasswordDbFixture.aForgottenPasswordDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class ForgottenPasswordResourceIT extends IntegrationTest {

    private static final String FORGOTTEN_PASSWORDS_RESOURCE_URL = "/v1/api/forgotten-passwords";

    @Test
    public void shouldGetForgottenPasswordReference_whenCreate_forAnExistingUser() throws Exception {

        String username = randomUuid();
        String email = username + "@example.com";
        userDbFixture(databaseHelper).withEmail(email).insertUser();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(Map.of("username", email)))
                .contentType(JSON)
                .accept(JSON)
                .post(FORGOTTEN_PASSWORDS_RESOURCE_URL)
                .then()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void shouldReturn404_whenCreate_forNonExistingUser() throws Exception {

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(Map.of("username", "non-existent-user")))
                .contentType(JSON)
                .accept(JSON)
                .post(FORGOTTEN_PASSWORDS_RESOURCE_URL)
                .then()
                .statusCode(NOT_FOUND.getStatusCode());

    }

    @Test
    public void shouldGetForgottenPassword_whenGetByCode_forAnExistingForgottenPassword() {

        String username = randomUuid();
        String email = username + "@example.com";
        int userId = userDbFixture(databaseHelper).withEmail(email).insertUser().getId();

        String forgottenPasswordCode = aForgottenPasswordDbFixture()
                .withDatabaseTestHelper(databaseHelper)
                .withUserId(userId)
                .insert()
                .getForgottenPasswordCode();

        givenSetup()
                .when()
                .accept(JSON)
                .get(FORGOTTEN_PASSWORDS_RESOURCE_URL + "/" + forgottenPasswordCode)
                .then()
                .statusCode(OK.getStatusCode())
                .body("code", is(forgottenPasswordCode));
    }

    @Test
    public void shouldReturn404_whenGetByCode_forNonExistingForgottenPassword() {

        givenSetup()
                .when()
                .accept(JSON)
                .get(FORGOTTEN_PASSWORDS_RESOURCE_URL + "/non-existent-code")
                .then()
                .statusCode(NOT_FOUND.getStatusCode());

    }

    @Test
    public void shouldReturn404_whenGetByCode_andCodeExceedsMaxLength() {

        givenSetup()
                .when()
                .accept(JSON)
                .get(FORGOTTEN_PASSWORDS_RESOURCE_URL + "/" + randomAlphanumeric(256))
                .then()
                .statusCode(NOT_FOUND.getStatusCode());

    }
}
