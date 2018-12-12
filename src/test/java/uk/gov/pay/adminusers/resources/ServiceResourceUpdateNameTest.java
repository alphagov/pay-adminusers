package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Service;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;

public class ServiceResourceUpdateNameTest extends IntegrationTest {
    
    private String serviceExternalId;

    @Before
    public void setup() {
        Service service = serviceDbFixture(databaseHelper).insertService();
        serviceExternalId = service.getExternalId();
    }
    @Test
    public void shouldUpdateBothNameAndEnglishServiceName_whenUpdatingName() {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("op", "replace", "path", "name", "value", "New Service Name"));
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
