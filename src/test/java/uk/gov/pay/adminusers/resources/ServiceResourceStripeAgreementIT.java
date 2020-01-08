package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.fixtures.StripeAgreementDbFixture;
import uk.gov.pay.adminusers.model.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.model.StripeAgreement.FIELD_IP_ADDRESS;
import static uk.gov.pay.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;

public class ServiceResourceStripeAgreementIT extends IntegrationTest {

    private Service service;

    @Before
    public void setUp() {
        service = serviceDbFixture(databaseHelper).insertService();
    }

    @Test
    public void shouldCreateStripeAgreement() {
        JsonNode payload = new ObjectMapper().valueToTree(Map.of(FIELD_IP_ADDRESS, "0.0.0.0"));
        givenSetup()
                .when()
                .accept(JSON)
                .body(payload)
                .post(format("/v1/api/services/%s/stripe-agreement", service.getExternalId()))
                .then()
                .statusCode(201);
    }

    @Test
    public void shouldReturn_NOT_FOUND_whenServiceNotFound() {
        JsonNode payload = new ObjectMapper().valueToTree(Map.of(FIELD_IP_ADDRESS, "0.0.0.0"));
        givenSetup()
                .when()
                .accept(JSON)
                .body(payload)
                .post(format("/v1/api/services/%s/stripe-agreement", "123"))
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldReturn_409_whenStripeAgreementAlreadyExists() {
        StripeAgreementDbFixture.stripeAgreementDbFixture(databaseHelper)
                .withServiceId(service.getId())
                .insert();

        JsonNode payload = new ObjectMapper().valueToTree(Map.of(FIELD_IP_ADDRESS, "0.0.0.0"));
        givenSetup()
                .when()
                .accept(JSON)
                .body(payload)
                .post(format("/v1/api/services/%s/stripe-agreement", service.getExternalId()))
                .then()
                .statusCode(409)
                .body("errors", hasSize(1))
                .body("errors[0]", is("Stripe agreement information is already stored for this service"));
    }

    @Test
    public void shouldReturn_422_whenProvidedInvalidIPAddress() {
        JsonNode payload = new ObjectMapper().valueToTree(Map.of(FIELD_IP_ADDRESS, "257.0.0.0"));
        givenSetup()
                .when()
                .accept(JSON)
                .body(payload)
                .post(format("/v1/api/services/%s/stripe-agreement", service.getExternalId()))
                .then()
                .statusCode(422)
                .body("errors", hasSize(1))
                .body("errors[0]", is("ipAddress must be valid IP address"));
    }

    @Test
    public void shouldReturn_422_whenIpAddressIsNotAString() {
        JsonNode payload = new ObjectMapper().valueToTree(Map.of(FIELD_IP_ADDRESS, 100));
        givenSetup()
                .when()
                .accept(JSON)
                .body(payload)
                .post(format("/v1/api/services/%s/stripe-agreement", service.getExternalId()))
                .then()
                .statusCode(422)
                .body("errors", hasSize(1))
                .body("errors[0]", is("ipAddress must be valid IP address"));
    }

    @Test
    public void shouldReturn_422_whenIpAddressNotProvided() {
        JsonNode payload = new ObjectMapper().valueToTree(emptyMap());
        givenSetup()
                .when()
                .accept(JSON)
                .body(payload)
                .post(format("/v1/api/services/%s/stripe-agreement", service.getExternalId()))
                .then()
                .statusCode(422)
                .body("errors", hasSize(1))
                .body("errors[0]", is("ipAddress must not be null"));
    }

    @Test
    public void shouldGetStripeAgreementDetails() {
        ZonedDateTime agreementTime = ZonedDateTime.now(ZoneId.of("UTC"));
        String ipAddress = "192.0.2.0";

        StripeAgreementDbFixture.stripeAgreementDbFixture(databaseHelper)
                .withServiceId(service.getId())
                .withIpAddress(ipAddress)
                .withAgreementTime(agreementTime)
                .insert();

        givenSetup()
                .when()
                .get(format("/v1/api/services/%s/stripe-agreement", service.getExternalId()))
                .then()
                .statusCode(200)
                .body("ip_address", is(ipAddress))
                .body("agreement_time", is(ISO_INSTANT_MILLISECOND_PRECISION.format(agreementTime)));
    }

    @Test
    public void shouldReturn_NOT_FOUND_whenStripeAgreementDoesNotExist() {
        givenSetup()
                .when()
                .accept(JSON)
                .get(format("/v1/api/services/%s/stripe-agreement", service.getExternalId()))
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldReturn_NOT_FOUND_whenServiceDoesNotExist() {
        givenSetup()
                .when()
                .accept(JSON)
                .get(format("/v1/api/services/%s/stripe-agreement", "NON_EXISTENT_SERVICE"))
                .then()
                .statusCode(404);
    }
}
