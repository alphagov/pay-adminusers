package uk.gov.pay.adminusers.resources;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.User;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class UserResourceFindTest extends IntegrationTest {

    private String username;

    @Before
    public void createAUser() {
        User user = userDbFixture(databaseHelper).insertUser();
        username = user.getUsername();
    }

    @Test
    public void shouldFindSuccessfully_existingUserByUserName() throws Exception {
        givenSetup()
                .when()
                .contentType(JSON)
                .get(format("%s?username=%s", USERS_RESOURCE_URL,username))
                .then()
                .statusCode(200)
                .body("username", is(username));
    }

    @Test
    public void shouldError404_ifUserNotFound() throws Exception {
        givenSetup()
                .when()
                .contentType(JSON)
                .get(format("%s?username=%s", USERS_RESOURCE_URL,"unknown-user@somewhere.com"))
                .then()
                .statusCode(404);

    }
}
