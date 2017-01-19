package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.ValidatableResponse;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.model.User;

import java.time.temporal.ChronoUnit;
import java.util.Map;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.pay.adminusers.utils.DateTimeUtils.toUTCZonedDateTime;

public class ForgottenPasswordResourceTest extends IntegrationTest {

    private static final String FORGOTTEN_PASSWORDS_RESOURCE_URL = "/v1/api/forgotten-passwords";

    private ObjectMapper mapper;

    @Before
    public void before() throws Exception {
        mapper = new ObjectMapper();
    }

    @Test
    public void shouldGetForgottenPasswordReference_whenCreate_forAnExistingUser() throws Exception {
        String random = randomUUID().toString();
        User user = aUser(random);
        databaseTestHelper.add(user);

        Map<String, String> forgottenPasswordPayload = ImmutableMap.of("username", user.getUsername());
        ValidatableResponse validatableResponse = givenSetup()
                .when()
                .body(mapper.writeValueAsString(forgottenPasswordPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(FORGOTTEN_PASSWORDS_RESOURCE_URL)
                .then()
                .statusCode(201);

        validatableResponse
                .body("username", is(user.getUsername()))
                .body("code", is(notNullValue()))
                .body("_links", hasSize(1))
                .body("_links[0].href", is("http://localhost:8080/v1/api/forgotten-passwords/" + validatableResponse.extract().body().path("code").toString()))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("self"));

        String dateTimeString = validatableResponse.extract().body().path("date");
        assertThat(toUTCZonedDateTime(dateTimeString).get(), within(1, ChronoUnit.MINUTES, now()));
    }

    @Test
    public void shouldReturn404_whenCreate_forNonExistingUser() throws Exception {

        Map<String, String> forgottenPasswordPayload = ImmutableMap.of("username", "non-existent-user");
        givenSetup()
                .when()
                .body(mapper.writeValueAsString(forgottenPasswordPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(FORGOTTEN_PASSWORDS_RESOURCE_URL)
                .then()
                .statusCode(404);

    }

    @Test
    public void shouldGetForgottenPassword_whenGetByCode_forAnExistingForgottenPassword() throws Exception {
        String random = randomUUID().toString();
        User user = aUser(random);
        ForgottenPassword forgottenPassword = aForgottenPassword(random);
        databaseTestHelper.add(user);
        databaseTestHelper.add(forgottenPassword, user.getId());

        givenSetup()
                .when()
                .accept(JSON)
                .get(FORGOTTEN_PASSWORDS_RESOURCE_URL + "/" + random)
                .then()
                .statusCode(200)
                .body("code", is(random));

    }

    @Test
    public void shouldReturn404_whenGetByCode_forNonExistingForgottenPassword() throws Exception {

        givenSetup()
                .when()
                .accept(JSON)
                .get(FORGOTTEN_PASSWORDS_RESOURCE_URL + "/non-existent-code")
                .then()
                .statusCode(404);

    }

    private ForgottenPassword aForgottenPassword(String random) {
        return ForgottenPassword.forgottenPassword(format("%s", random), format("%s-name", random));
    }


    private User aUser(String random) {
        return User.from(format("%s-name", random), format("%s-password", random), format("%s@email.com", random), "1", "784rh", "8948924");
    }
}
