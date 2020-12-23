package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.User;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

class UserResourcePatchIT extends IntegrationTest {

    private String externalId;

    @BeforeEach
    void createAUser() {
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withUsername(username).withEmail(email).insertUser();

        externalId = user.getExternalId();
    }

    @Test
    void shouldIncreaseSessionVersion_whenPatchAttempt() {

        JsonNode payload = mapper.valueToTree(Map.of("op", "append", "path", "sessionVersion", "value", 2));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .patch(format(USER_RESOURCE_URL, externalId))
                .then()
                .statusCode(200)
                .body("session_version", is(2));
    }

    @Test
    void shouldUpdateTelephoneNumber_whenPatchAttempt() {

        String newTelephoneNumber = "+441134960000";
        JsonNode payload = mapper.valueToTree(Map.of("op", "replace", "path", "telephone_number", "value", newTelephoneNumber));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .patch(format(USER_RESOURCE_URL, externalId))
                .then()
                .statusCode(200)
                .body("telephone_number", is(newTelephoneNumber));

    }

    @Test
    void shouldUpdateFeatures_whenPatchAttempt() {

        String newFeatures = "SUPER_FEATURE_1, SECRET_SQUIRREL_FEATURE";
        JsonNode payload = mapper.valueToTree(Map.of("op", "replace", "path", "features", "value", newFeatures));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .patch(format(USER_RESOURCE_URL, externalId))
                .then()
                .statusCode(200)
                .body("features", is(newFeatures));
    }

    @Test
    void shouldDisableUser_whenPatchAttempt() {

        JsonNode payload = mapper.valueToTree(Map.of("op", "replace", "path", "disabled", "value", "true"));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .patch(format(USER_RESOURCE_URL, externalId))
                .then()
                .statusCode(200)
                .body("disabled", is(true));
    }

    @Test
    void shouldReturn404_whenUnknownExternalIdIsSupplied() {

        JsonNode payload = mapper.valueToTree(Map.of("op", "append", "path", "sessionVersion", "value", 1));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .patch(format(USER_RESOURCE_URL, "whatever"))
                .then()
                .statusCode(404);
    }

    @Test
    void shouldError_whenPatchRequiredFieldsAreMissing() {

        JsonNode payload = mapper.valueToTree(Map.of("blah", "sessionVersion", "value", 1));

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .body(payload)
                .patch(format(USER_RESOURCE_URL, externalId))
                .then()
                .statusCode(400)
                .body("errors", hasSize(2))
                .body("errors[0]", is("Field [op] is required"))
                .body("errors[1]", is("Field [path] is required"));
    }
}
