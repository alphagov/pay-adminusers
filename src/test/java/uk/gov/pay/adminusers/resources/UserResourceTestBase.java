package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;

import static com.jayway.restassured.http.ContentType.JSON;

public class UserResourceTestBase extends IntegrationTest {

    protected static final String USERS_RESOURCE_URL = "/v1/api/users";
    protected static final String USER_RESOURCE_URL = "/v1/api/users/%s";
    protected static final String USERS_AUTHENTICATE_URL = "/v1/api/users/authenticate";
    protected static final String LOGIN_ATTEMPT_URL = USER_RESOURCE_URL + "/attempt-login";

    protected ObjectMapper mapper;

    @Before
    public void before() throws Exception {
        mapper = new ObjectMapper();
    }

    protected void createAValidUser(String random) throws JsonProcessingException {
        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", "user-" + random)
                .put("password", "password-" + random)
                .put("email", "user-" + random + "@example.com")
                .put("gateway_account_id", "1")
                .put("telephone_number", "45334534634")
                .put("otp_key", "34f34")
                .put("role_name", "admin")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(userPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_RESOURCE_URL)
                .then()
                .statusCode(201);
    }
}
