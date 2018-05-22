package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Service;

import java.util.Map;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class ServiceResourceUpdateMerchantDetailsTest extends IntegrationTest {

    @Test
    public void shouldSuccess_whenUpdatingMerchantDetails() throws Exception {
        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, "existing-name");
        databaseHelper.addService(service, randomInt().toString());
        Map<String, Object> payload = ImmutableMap.<String, Object>builder()
                .put("name", "somename")
                .put("telephone_number", "03069990000")
                .put("address_line1", "line1")
                .put("address_line2", "line2")
                .put("address_city", "city")
                .put("address_country", "country")
                .put("address_postcode", "postcode")
                .put("email", "dd-merchant@example.com")
                .build();

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
        Service service = Service.from(randomInt(), serviceExternalId, "existing-name");
        databaseHelper.addService(service, randomInt().toString());
        Map<String, Object> payload = ImmutableMap.<String, Object>builder()
                .put("name", "somename")
                .put("address_line1", "line1")
                .put("address_city", "city")
                .put("address_country", "country")
                .put("address_postcode", "postcode")
                .build();

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
        Service service = Service.from(randomInt(), serviceExternalId, "existing-name");
        databaseHelper.addService(service, randomInt().toString());
        Map<String, Object> payload = ImmutableMap.<String, Object>builder()
                .put("telephone_number", "03069990000")
                .put("address_line1", "line1")
                .put("address_line2", "line2")
                .put("address_city", "city")
                .put("address_country", "country")
                .put("address_postcode", "postcode")
                .build();

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

}
