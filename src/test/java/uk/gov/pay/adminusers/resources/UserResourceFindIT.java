package uk.gov.pay.adminusers.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.User;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class UserResourceFindIT extends IntegrationTest {

    private String username;

    @BeforeEach
    public void createAUser() {
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withUsername(username).withEmail(email).insertUser();
        this.username = user.getUsername();
    }

    @Test
    public void shouldFindSuccessfully_existingUserByUserName() throws Exception {

        Map<String, String> findPayload = Map.of("username", username);

        givenSetup()
                .when()
                .contentType(JSON)
                .body(mapper.writeValueAsString(findPayload))
                .post(FIND_RESOURCE_URL)
                .then()
                .statusCode(200)
                .body("username", is(username));
    }

    @Test
    public void shouldError404_ifUserNotFound() throws Exception {
        Map<String, String> findPayload = Map.of("username", "unknown-user@somewhere.com");

        givenSetup()
                .when()
                .contentType(JSON)
                .body(mapper.writeValueAsString(findPayload))
                .post(FIND_RESOURCE_URL)
                .then()
                .statusCode(404);

    }

    @Test
    public void shouldError400_ifFieldsMissing() throws Exception {
        Map<String, String> findPayload = Map.of("", "");

        givenSetup()
                .when()
                .contentType(JSON)
                .body(mapper.writeValueAsString(findPayload))
                .post(FIND_RESOURCE_URL)
                .then()
                .statusCode(400);

    }
}
