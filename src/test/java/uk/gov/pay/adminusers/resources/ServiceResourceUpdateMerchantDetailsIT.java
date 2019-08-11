package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceName;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;

public class ServiceResourceUpdateMerchantDetailsIT extends IntegrationTest {

    @Test
    public void shouldSuccess_whenUpdatingMerchantDetails() throws Exception {
        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, new ServiceName("existing-name"));
        databaseHelper.addService(service, randomInt().toString());
        Map<String, Object> payload = Map.of(
                "name", "somename",
                "telephone_number", "03069990000",
                "address_line1", "line1",
                "address_line2", "line2",
                "address_city", "city",
                "address_country", "country",
                "address_postcode", "postcode",
                "email", "dd-merchant@example.com");

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .put(format(SERVICE_RESOURCE, serviceExternalId) + "/merchant-details")
                .then()
                .statusCode(200)
                .body("merchant_details.name", is("somename"))
                .body("merchant_details.telephone_number", is("03069990000"))
                .body("merchant_details.address_line1", is("line1"))
                .body("merchant_details.address_line2", is("line2"))
                .body("merchant_details.address_city", is("city"))
                .body("merchant_details.address_country", is("country"))
                .body("merchant_details.address_postcode", is("postcode"))
                .body("merchant_details.email", is("dd-merchant@example.com"));
    }

    @Test
    public void shouldSuccess_whenUpdatingMerchantDetails_withoutOptionalFields() throws Exception {
        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, new ServiceName("existing-name"));
        databaseHelper.addService(service, randomInt().toString());
        Map<String, Object> payload = Map.of(
                "name", "somename",
                "address_line1", "line1",
                "address_city", "city",
                "address_country", "country",
                "address_postcode", "postcode");

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .put(format(SERVICE_RESOURCE, serviceExternalId) + "/merchant-details")
                .then()
                .statusCode(200)
                .body("merchant_details.name", is("somename"))
                .body("merchant_details.address_line1", is("line1"))
                .body("merchant_details.address_city", is("city"))
                .body("merchant_details.address_country", is("country"))
                .body("merchant_details.address_postcode", is("postcode"))
                .body("merchant_details", not(hasKey("telephone_number")))
                .body("merchant_details", not(hasKey("address_line2")))
                .body("merchant_details", not(hasKey("email")));
    }

    @Test
    public void shouldFail_whenUpdatingMerchantDetails_withMissingMandatoryFieldName() throws Exception {
        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, new ServiceName("existing-name"));
        databaseHelper.addService(service, randomInt().toString());
        Map<String, Object> payload = Map.of(
                "telephone_number", "03069990000",
                "address_line1", "line1",
                "address_line2", "line2",
                "address_city", "city",
                "address_country", "country",
                "address_postcode", "postcode");

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .put(format(SERVICE_RESOURCE, serviceExternalId) + "/merchant-details")
                .then()
                .statusCode(400)
                .body("errors", hasSize(1))
                .body("errors", hasItems("Field [name] is required"));
    }

    @Test
    public void shouldSucceed_whenPatchUpdatingMultipleMerchantDetails() {
        String serviceExternalId = serviceDbFixture(databaseHelper).insertService().getExternalId();
        String addressLine1 = "1 Spider Lane";
        String addressCountry = "Somewhere";
        ArrayNode payload = mapper.createArrayNode();

        payload.add(mapper.valueToTree(Map.of(
                "op", "replace",
                "path", "merchant_details/address_line1",
                "value", addressLine1)));

        payload.add(mapper.valueToTree(Map.of(
                "op", "replace",
                "path", "merchant_details/address_country",
                "value", addressCountry)));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .patch(format(SERVICE_RESOURCE, serviceExternalId))
                .then()
                .statusCode(200)
                .body("merchant_details.address_line1", is(addressLine1))
                .body("merchant_details.address_country", is(addressCountry));
    }

    @Test
    public void shouldSucceed_whenPatchUpdatingAddressLine1AndMerchantDetailsIsNull() {
        String serviceExternalId = serviceDbFixture(databaseHelper)
                .withMerchantDetails(null)
                .insertService()
                .getExternalId();

        String addressLine1 = "1 Spider Lane";
        JsonNode payload = mapper.valueToTree(Map.of(
                "op", "replace",
                "path", "merchant_details/address_line1",
                "value", addressLine1));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .patch(format(SERVICE_RESOURCE, serviceExternalId))
                .then()
                .statusCode(200)
                .body("merchant_details.address_line1", is(addressLine1));
    }
}
