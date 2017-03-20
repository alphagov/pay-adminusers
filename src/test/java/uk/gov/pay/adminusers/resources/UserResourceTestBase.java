package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;

import java.util.UUID;

import static com.jayway.restassured.http.ContentType.JSON;

public class UserResourceTestBase extends IntegrationTest {

    protected static final String USERS_RESOURCE_URL = "/v1/api/users";
    protected static final String USER_RESOURCE_URL = "/v1/api/users/%s";
    protected static final String USERS_AUTHENTICATE_URL = "/v1/api/users/authenticate";
    protected static final String USER_2FA_URL = "/v1/api/users/%s/second-factor";

    protected ObjectMapper mapper;

    @Before
    public void before() throws Exception {
        mapper = new ObjectMapper();
    }

    protected String createAValidUser() throws JsonProcessingException {

        String username = RandomStringUtils.randomAlphanumeric(10) + UUID.randomUUID();
        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", username)
                .put("password", "password-" + username)
                .put("email", "user-" + username + "@example.com")
                .put("gateway_account_ids", new String[]{"1", "2"})
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

        return username;
    }
}
