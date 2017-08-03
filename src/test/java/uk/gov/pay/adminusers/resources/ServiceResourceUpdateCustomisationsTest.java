package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceCustomisations;

import java.util.Map;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class ServiceResourceUpdateCustomisationsTest extends IntegrationTest {

    @Test
    public void shouldSuccess_whenUpdatingServiceCustomisations_withValidValues() throws Exception {

        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, "existing-name");
        databaseHelper.addService(service, randomInt().toString());

        Map<String, String> payload = ImmutableMap.of("banner_colour", "red", "logo_url", "http://some.url/image.gif");

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .post(format(SERVICES_RESOURCE + "/%s/customise", serviceExternalId))
                .then()
                .statusCode(200)
                .body("service_customisations.banner_colour", is("red"))
                .body("service_customisations.logo_url", is("http://some.url/image.gif"));

    }

    @Test
    public void shouldReplaceWithEmpty_whenUpdatingServiceCustomisations_withEmptyValues() throws Exception {
        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, "existing-name");
        service.setServiceCustomisations(new ServiceCustomisations("red","http://some.url/image.gif"));
        databaseHelper.addService(service, randomInt().toString());

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .body(mapper.writeValueAsString(""))
                .post(format(SERVICES_RESOURCE + "/%s/customise", serviceExternalId))
                .then()
                .statusCode(200)
                .body("service_customisations.banner_colour", is(""))
                .body("service_customisations.logo_url", is(""));

    }

    @Test
    public void shouldReturn404_whenUpdatingServiceCustomisations_ifNotFound() throws Exception {

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .body(mapper.writeValueAsString(""))
                .post(format(SERVICES_RESOURCE + "/%s/customise", "non-existent-id"))
                .then()
                .statusCode(404);
    }


}
