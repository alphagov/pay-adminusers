package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.Service;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;

class ServiceResourceUpdateNameIT extends IntegrationTest {

    private String serviceExternalId;

    @BeforeEach
    void setUp() {
        Service service = serviceDbFixture(databaseHelper).insertService();
        serviceExternalId = service.getExternalId();
    }
    @Test
    void shouldUpdateBothNameAndEnglishServiceName_whenUpdatingEnglishName() {
        JsonNode payload = mapper.valueToTree(Map.of("op", "replace", "path", "service_name/en", "value", "New Service Name"));
        givenSetup()
                .when()
                .accept(JSON)
                .body(payload)
                .patch(format("/v1/api/services/%s", serviceExternalId))
                .then()
                .statusCode(200)
                .body("name", is("New Service Name"))
                .body("service_name.en", is("New Service Name"));
    }
}
