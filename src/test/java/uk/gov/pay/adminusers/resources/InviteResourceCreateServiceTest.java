package uk.gov.pay.adminusers.resources;


import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.http.ContentType;
import org.junit.Test;
import uk.gov.pay.adminusers.fixtures.UserDbFixture;

import static javax.ws.rs.core.Response.Status.*;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;

public class InviteResourceCreateServiceTest extends IntegrationTest {

    public static final String SERVICE_INVITES_CREATE_URL = "/v1/api/invites/service";

    @Test
    public void shouldSuccess_WhenAllRequiredFieldsAreProvidedAndValid() throws Exception {
        String email = "example@example.gov.uk";
        String telephoneNumber = "088882345689";
        ImmutableMap<String, String> payload = ImmutableMap.of("telephone_number", telephoneNumber, "email", email, "password", "plain_text_password");

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .contentType(ContentType.JSON)
                .post(SERVICE_INVITES_CREATE_URL)
                .then()
                .statusCode(CREATED.getStatusCode())
                .body("email", is(email.toLowerCase()))
                .body("telephone_number", is(telephoneNumber))
                .body("_links", hasSize(2))
                .body("_links[0].href", matchesPattern("^https://selfservice.pymnt.localdomain/invites/[0-9a-z]{32}$"))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("invite"));
    }

    @Test
    public void shouldFail_WhenMandatoryFieldsAreMissing() throws Exception {
        String telephoneNumber = "088882345689";
        ImmutableMap<String, String> payload = ImmutableMap.of("telephone_number", telephoneNumber, "password", "plain_text_password");

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .contentType(ContentType.JSON)
                .post(SERVICE_INVITES_CREATE_URL)
                .then()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("errors", hasSize(1))
                .body("errors", hasItems("Field [email] is required"));
    }

    @Test
    public void shouldFail_WhenEmailIsNotPublicSectorDomain() throws Exception {
        String email = "example@example.co.uk";
        String telephoneNumber = "088882345689";
        ImmutableMap<String, String> payload = ImmutableMap.of("telephone_number", telephoneNumber, "email", email, "password", "plain_text_password");

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .contentType(ContentType.JSON)
                .post(SERVICE_INVITES_CREATE_URL)
                .then()
                .statusCode(FORBIDDEN.getStatusCode())
                .body("errors", hasSize(1))
                .body("errors", hasItems("Email [example@example.co.uk] is not a valid public sector email"));
    }

    @Test
    public void shouldFail_WhenEmailIsAlreadyRegistered() throws Exception {

        String email = "example@example.gov.uk";
        UserDbFixture.userDbFixture(databaseHelper).withEmail(email).insertUser();

        String telephoneNumber = "088882345689";
        ImmutableMap<String, String> payload = ImmutableMap.of("telephone_number", telephoneNumber, "email", email, "password", "plain_text_password");

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(payload))
                .contentType(ContentType.JSON)
                .post(SERVICE_INVITES_CREATE_URL)
                .then()
                .statusCode(CONFLICT.getStatusCode())
                .body("errors", hasSize(1))
                .body("errors", hasItems("email [example@example.gov.uk] already exists"));
    }

}
