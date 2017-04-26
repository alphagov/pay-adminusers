package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.User;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class UserResourcePatchTest extends IntegrationTest {

    private String externalId;

    @Before
    public void createAUser() {
        User user = userDbFixture(databaseHelper).insertUser();

        externalId = user.getExternalId();
    }

    @Test
    public void shouldIncreaseSessionVersion_whenPatchAttempt() throws Exception {

        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("op", "append", "path", "sessionVersion", "value", 2));

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
    public void shouldDisableUser_whenPatchAttempt() throws Exception {

        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("op", "replace", "path", "disabled", "value", "true"));

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
    public void shouldReturn404_whenUnknownExternalIdIsSupplied() throws Exception {

        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("op", "append", "path", "sessionVersion", "value", 1));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .patch(format(USER_RESOURCE_URL, "whatever"))
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldError_whenPatchRequiredFieldsAreMissing() throws Exception {

        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("blah", "sessionVersion", "value", 1));

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
