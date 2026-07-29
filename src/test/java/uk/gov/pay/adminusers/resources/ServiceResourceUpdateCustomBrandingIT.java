package uk.gov.pay.adminusers.resources;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;

class ServiceResourceUpdateCustomBrandingIT extends IntegrationTest {

    @Test
    void shouldSuccess_whenUpdatingCustomBranding() throws Exception {
        var serviceExternalId = serviceDbFixture(databaseHelper).insertService().getExternalId();

        Map<String, Object> payload = Map.of("path", "custom_branding", "op", "replace", "value", Map.of("image_url","image url","css_url","css url"));

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
    void shouldReplaceWithEmpty_whenUpdatingCustomBranding_withEmptyObject() throws Exception {
        var service = serviceDbFixture(databaseHelper).insertService();
        Map<String, Object> existingBranding = Map.of("css_url","existing css", "image_url","existing image");
        service.setCustomBranding(existingBranding);

        Map<String, Object> payloadWithEmptyBranding = Map.of("path", "custom_branding", "op", "replace", "value", emptyMap());
        
        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .body(mapper.writeValueAsString(payloadWithEmptyBranding))
                .patch(format(SERVICE_RESOURCE, service.getExternalId()))
                .then()
                .statusCode(200)
                .body("custom_branding", is(nullValue()));

    }

    @Test
    void shouldReturn400_whenUpdatingServiceCustomisations_ifPayloadNotJson() throws Exception {
        var service = serviceDbFixture(databaseHelper).insertService();
        Map<String, Object> customBranding = Map.of("css_url","existing css", "image_url","existing image");
        service.setCustomBranding(customBranding);

        Map<String, Object> payload = Map.of("path", "custom_branding", "op", "replace", "value", "blah");

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .patch(format(SERVICE_RESOURCE, service.getExternalId()))
                .then()
                .statusCode(400);
    }

    @Test
    void shouldReturn404_whenUpdatingServiceCustomisations_ifNotFound() throws Exception {

        Map<String, Object> payload = Map.of("path", "custom_branding", "op", "replace", "value", Map.of("image_url","image url","css_url","css url"));

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
