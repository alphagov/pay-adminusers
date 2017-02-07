package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.ValidatableResponse;
import org.junit.Test;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class UserResourcePatchTest extends UserResourceTestBase {

    @Test
    public void shouldIncreaseSessionVersion_whenPatchAttempt() throws Exception {
        String random = randomUUID().toString();
        createAValidUser(random);
        String username = "user-" + random;

        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("op", "append", "path", "sessionVersion", "value", 2));

        ValidatableResponse validatableResponse = givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .patch(format(USER_RESOURCE_URL, username))
                .then()
                .statusCode(200)
                .body("session_version", is(2));
    }

    @Test
    public void shouldDisableUser_whenPatchAttempt() throws Exception {
        String random = randomUUID().toString();
        createAValidUser(random);
        String username = "user-" + random;

        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("op", "replace", "path", "disabled", "value", "true"));

        ValidatableResponse validatableResponse = givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .patch(format(USER_RESOURCE_URL, username))
                .then()
                .statusCode(200)
                .body("disabled", is(true));
    }

    @Test
    public void shouldReturn404_whenUnknownUsernameIsSupplied() throws Exception {
        String random = randomUUID().toString();
        createAValidUser(random);

        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("op", "append", "path", "sessionVersion", "value", 1));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .patch(format(USER_RESOURCE_URL, random))
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldError_whenPatchRequiredFieldsAreMissing() throws Exception {
        String random = randomUUID().toString();
        createAValidUser(random);
        String username = "user-" + random;

        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("blah", "sessionVersion", "value", 1));

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .body(payload)
                .patch(format(USER_RESOURCE_URL, username))
                .then()
                .statusCode(400)
                .body("errors", hasSize(2))
                .body("errors[0]", is("Field [op] is required"))
                .body("errors[1]", is("Field [path] is required"));
    }
}
