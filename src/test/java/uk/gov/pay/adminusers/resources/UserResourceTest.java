package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.User;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;

public class UserResourceTest extends IntegrationTest {

    private static final String USERS_RESOURCE_URL = "/v1/api/users";

    private ObjectMapper mapper;

    @Before
    public void before() throws Exception {
        mapper = new ObjectMapper();
    }

    @Test
    public void shouldCreateAUserSuccessfully() throws Exception {
        String random = randomUUID().toString();
        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", "user-" + random)
                .put("email", "user-" + random + "@example.com")
                .put("gatewayAccountId", "1")
                .put("telephoneNumber", "45334534634")
                .put("otpKey", "34f34")
                .put("roleName", "admin")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(userPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_RESOURCE_URL)
                .then()
                .statusCode(201)
                .body("id", nullValue())
                .body("username", is("user-" + random))
                .body("password", nullValue())
                .body("email", is("user-" + random + "@example.com"))
                .body("gatewayAccountId", is("1"))
                .body("telephoneNumber", is("45334534634"))
                .body("otpKey", is("34f34"))
                .body("loginCount", is(0))
                .body("disabled", is(false))
                .body("roles", hasSize(1))
                .body("roles[0].name", is("admin"))
                .body("roles[0].permissions", hasSize(27)); //we could consider removing this assertion if the permissions constantly changing
    }

    @Test
    public void shouldError400_IfRoleDoesNotExist() throws Exception {
        String random = randomUUID().toString();
        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", "user-" + random)
                .put("email", "user-" + random + "@example.com")
                .put("gatewayAccountId", "1")
                .put("telephoneNumber", "45334534634")
                .put("otpKey", "34f34")
                .put("roleName", "invalid-role")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(userPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_RESOURCE_URL)
                .then()
                .statusCode(400)
                .body("errors", hasSize(1))
                .body("errors[0]", is("role [invalid-role] not recognised"));
    }

    @Test
    public void shouldError400_whenFieldsMissingForUserCreation() throws Exception {
        ImmutableMap<Object, Object> invalidPayload = ImmutableMap.builder()
                .put("gatewayAccountId", "1")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invalidPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_RESOURCE_URL)
                .then()
                .statusCode(400)
                .body("errors", hasSize(4))
                .body("errors", hasItems(
                        "Field [username] is required",
                        "Field [email] is required",
                        "Field [telephoneNumber] is required",
                        "Field [roleName] is required"));
    }

    @Test
    public void shouldError409_IfUsernameAlreadyExists() throws Exception {

        String random = randomUUID().toString();
        String username = "user-" + random;
        User user = User.from(username, "password", "email@example.com", "2", "otpKey", "3543534");
        databaseTestHelper.add(user);

        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", username)
                .put("email", "user-" + random + "@example.com")
                .put("gatewayAccountId", "1")
                .put("telephoneNumber", "45334534634")
                .put("roleName", "admin")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(userPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_RESOURCE_URL)
                .then()
                .statusCode(409)
                .body("errors", hasSize(1))
                .body("errors[0]", is(format("username [%s] already exists", username)));
    }
}
