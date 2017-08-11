package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Service;

import java.util.Map;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class ServiceResourceUpdateCustomBrandingTest extends IntegrationTest {

    @Test
    public void shouldSuccess_whenUpdatingCustomBranding() throws Exception {

        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, "existing-name");
        databaseHelper.addService(service, randomInt().toString());

        Map<String, Object> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", ImmutableMap.of("image_url","image url","css_url","css url"));

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .patch(format(SERVICE_RESOURCE, serviceExternalId))
                .then()
                .statusCode(200)
                .body("custom_branding.image_url", is("image url"))
                .body("custom_branding.css_url", is("css url"));

    }

    @Test
    public void shouldReplaceWithEmpty_whenUpdatingCustomBranding_withEmptyValue() throws Exception {
        String serviceExternalId = randomUuid();
        Map<String, Object> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", "");
        Service service = Service.from(randomInt(), serviceExternalId, "existing-name");
        service.setCustomBranding(payload);
        databaseHelper.addService(service, randomInt().toString());


        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .patch(format(SERVICE_RESOURCE, serviceExternalId))
                .then()
                .statusCode(200)
                .body("custom_branding", is(nullValue()));

    }

    @Test
    public void shouldReturn404_whenUpdatingServiceCustomisations_ifNotFound() throws Exception {

        Map<String, Object> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", ImmutableMap.of("image_url","image url","css_url","css url"));

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .patch(format(SERVICE_RESOURCE, "non-existent-id"))
                .then()
                .statusCode(404);
    }


}
