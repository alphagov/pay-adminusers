package uk.gov.pay.adminusers.resources;

import com.jayway.restassured.response.ValidatableResponse;
import org.junit.Test;

import java.time.temporal.ChronoUnit;

import static com.google.common.collect.ImmutableMap.of;
import static com.jayway.restassured.http.ContentType.JSON;
import static java.time.ZonedDateTime.now;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.pay.adminusers.fixtures.ForgottenPasswordDbFixture.forgottenPasswordDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;
import static uk.gov.pay.adminusers.utils.DateTimeUtils.toUTCZonedDateTime;

public class ForgottenPasswordResourceTest extends IntegrationTest {

    private static final String FORGOTTEN_PASSWORDS_RESOURCE_URL = "/v1/api/forgotten-passwords";

    @Test
    public void shouldGetForgottenPasswordReference_whenCreate_forAnExistingUser() throws Exception {

        String username = userDbFixture(databaseHelper).insertUser().getUsername();

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

        int userId = userDbFixture(databaseHelper).insertUser().getId();
        String forgottenPasswordCode = forgottenPasswordDbFixture(databaseHelper, userId).insertForgottenPassword();

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
