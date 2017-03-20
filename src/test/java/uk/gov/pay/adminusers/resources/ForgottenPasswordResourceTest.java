package uk.gov.pay.adminusers.resources;

import com.jayway.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import uk.gov.pay.adminusers.fixtures.ForgottenPasswordDbFixture;
import uk.gov.pay.adminusers.fixtures.UserDbFixture;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.model.User;

import java.time.temporal.ChronoUnit;

import static com.google.common.collect.ImmutableMap.of;
import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.utils.DateTimeUtils.toUTCZonedDateTime;

public class ForgottenPasswordResourceTest extends IntegrationTest {

    private static final String FORGOTTEN_PASSWORDS_RESOURCE_URL = "/v1/api/forgotten-passwords";

    @Test
    public void shouldGetForgottenPasswordReference_whenCreate_forAnExistingUser() throws Exception {

        String username = UserDbFixture.aUser(databaseTestHelper).build().getUsername();

        ValidatableResponse validatableResponse = givenSetup()
                .when()
                .body(mapper.writeValueAsString(of("username", username)))
                .contentType(JSON)
                .accept(JSON)
                .post(FORGOTTEN_PASSWORDS_RESOURCE_URL)
                .then()
                .statusCode(201);

        validatableResponse
                .body("username", is(username))
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

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(of("username", "non-existent-user")))
                .contentType(JSON)
                .accept(JSON)
                .post(FORGOTTEN_PASSWORDS_RESOURCE_URL)
                .then()
                .statusCode(404);

    }

    @Test
    public void shouldGetForgottenPassword_whenGetByCode_forAnExistingForgottenPassword() throws Exception {

        int userId = UserDbFixture.aUser(databaseTestHelper).build().getId();
        String forgottenPasswordCode = ForgottenPasswordDbFixture.aForgottenPassword(databaseTestHelper, userId).build();

        givenSetup()
                .when()
                .accept(JSON)
                .get(FORGOTTEN_PASSWORDS_RESOURCE_URL + "/" + forgottenPasswordCode)
                .then()
                .statusCode(200)
                .body("code", is(forgottenPasswordCode));

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

    @Test
    public void shouldReturn404_whenGetByCode_andCodeExceedsMaxLength() throws Exception {

        givenSetup()
                .when()
                .accept(JSON)
                .get(FORGOTTEN_PASSWORDS_RESOURCE_URL + "/" + randomAlphanumeric(256))
                .then()
                .statusCode(404);

    }
}
