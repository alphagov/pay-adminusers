package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Service;

import java.util.Map;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class ServiceResourceUpdateTest extends IntegrationTest {

    @Test
    public void shouldSuccess_whenReplaceServiceNameWithANewValue() throws Exception {

        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, "existing-name");
        databaseHelper.addService(service, randomInt().toString());

        Map<String, String> payload = ImmutableMap.of("op", "replace", "path", "name", "value", "updated-service-name");

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .patch(format("/v1/api/services/%s", serviceExternalId))
                .then()
                .statusCode(200)
                .body("name", is("updated-service-name"));

    }

    @Test
    public void shouldError404_ifServiceExternalIdDoesNotExist() throws Exception {
        Map<String, String> payload = ImmutableMap.of("op", "replace", "path", "name", "value", "new-service-name");

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .patch(format("/v1/api/services/%s", "non-existent-service-id"))
                .then()
                .statusCode(404);

    }

    @Test
    public void shouldError400_ifMandatoryFieldMissing() throws Exception {
        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, "existing-name");
        databaseHelper.addService(service, randomInt().toString());

        Map<String, String> payload = ImmutableMap.of("op", "replace", "path", "name");

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .patch(format("/v1/api/services/%s", "non-existent-service-id"))
                .then()
                .statusCode(400)
                .body("errors", hasSize(1))
                .body("errors", hasItems(format("Field [value] is required")));

    }

}
