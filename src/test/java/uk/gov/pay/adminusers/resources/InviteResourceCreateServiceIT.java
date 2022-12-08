package uk.gov.pay.adminusers.resources;


import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.fixtures.UserDbFixture;

import java.util.Locale;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

class InviteResourceCreateServiceIT extends IntegrationTest {

    public static final String SERVICE_INVITES_CREATE_URL = "/v1/api/invites/service";

    @Test
    void shouldSuccess_WhenAllRequiredFieldsAreProvidedAndValid() throws Exception {
        String email = "example@example.gov.uk";
        Map<String, String> payload = Map.of("email", email);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .contentType(ContentType.JSON)
                .post(SERVICE_INVITES_CREATE_URL)
                .then()
                .statusCode(CREATED.getStatusCode())
                .body("email", is(email.toLowerCase(Locale.ENGLISH)))
                .body("_links", hasSize(2))
                .body("_links[0].href", matchesPattern("^https://selfservice.pymnt.localdomain/invites/[0-9a-z]{32}$"))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("invite"));
    }

    @Test
    void shouldFail_WhenMandatoryFieldsAreMissing() throws Exception {
        Map<String, String> payload = Map.of();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .contentType(ContentType.JSON)
                .post(SERVICE_INVITES_CREATE_URL)
                .then()
                .statusCode(422)
                .body("errors", hasSize(1))
                .body("errors", hasItems("email must not be empty"));
    }

    @Test
    void shouldFail_WhenEmailIsInvalid() throws Exception {
        Map<String, String> payload = Map.of("email", "invalid");

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .contentType(ContentType.JSON)
                .post(SERVICE_INVITES_CREATE_URL)
                .then()
                .statusCode(422)
                .body("errors", hasSize(1))
                .body("errors", hasItems("email must be a well-formed email address"));
    }

    @Test
    void shouldFail_WhenEmailIsNotPublicSectorDomain() throws Exception {
        String email = "example@example.com";
        Map<String, String> payload = Map.of("email", email);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .contentType(ContentType.JSON)
                .post(SERVICE_INVITES_CREATE_URL)
                .then()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("errors", hasSize(1))
                .body("errors", hasItems("Email [" + email + "] is not a valid public sector email"));
    }

    @Test
    void shouldFail_WhenEmailIsAlreadyRegistered() throws Exception {

        String username = randomUuid();
        String email = username + "@example.gov.uk";
        UserDbFixture.userDbFixture(databaseHelper).withUsername(username).withEmail(email).insertUser();

        Map<String, String> payload = Map.of("email", email);

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .contentType(ContentType.JSON)
                .post(SERVICE_INVITES_CREATE_URL)
                .then()
                .statusCode(CONFLICT.getStatusCode())
                .body("errors", hasSize(1))
                .body("errors", hasItems("email [" + email + "] already exists"));
    }

}
