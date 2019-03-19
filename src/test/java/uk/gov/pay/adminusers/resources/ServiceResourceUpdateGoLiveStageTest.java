package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.pay.adminusers.model.GoLiveStage;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;

public class ServiceResourceUpdateGoLiveStageTest extends IntegrationTest {

    @Test
    public void shouldUpdateGoLiveStage() {
        String serviceExternalId = serviceDbFixture(databaseHelper).insertService().getExternalId();
        JsonNode payload = new ObjectMapper()
                .valueToTree(ImmutableMap.of(
                        "op", "replace",
                        "path", "current_go_live_stage",
                        "value", "CHOSEN_PSP_STRIPE"));

        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .patch(format(SERVICE_RESOURCE, serviceExternalId))
                .then()
                .statusCode(200)
                .body("current_go_live_stage", is(valueOf(GoLiveStage.CHOSEN_PSP_STRIPE)));
    }
}
