package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.GoLiveStage;

import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;

public class ServiceResourceUpdateIT extends IntegrationTest {

    @Test
    public void shouldUpdateServiceFields() {
        String serviceExternalId = serviceDbFixture(databaseHelper).insertService().getExternalId();

        JsonNode payload = mapper
                .valueToTree(List.of(
                        patchRequest("replace", "current_go_live_stage", "CHOSEN_PSP_STRIPE"),
                        patchRequest("replace", "redirect_to_service_immediately_on_terminal_state", true),
                        patchRequest("replace", "experimental_features_enabled", true),
                        patchRequest("replace", "collect_billing_address", true),
                        patchRequest("replace", "sector", "local government"),
                        patchRequest("replace", "internal", true),
                        patchRequest("replace", "archived", true),
                        patchRequest("replace", "went_live_date", "2020-01-01T01:01:00Z")
                ));
        
        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .patch(format(SERVICE_RESOURCE, serviceExternalId))
                .then()
                .statusCode(200)
                .body("current_go_live_stage", is(valueOf(GoLiveStage.CHOSEN_PSP_STRIPE)))
                .body("redirect_to_service_immediately_on_terminal_state", is(true))
                .body("experimental_features_enabled", is(true))
                .body("collect_billing_address", is(true))
                .body("sector", is("local government"))
                .body("internal", is(true))
                .body("archived", is(true))
                .body("went_live_date", is("2020-01-01T01:01:00.000Z"));
                
    }

    private Map<String, Object> patchRequest(String op, String path, Object value) {
        return Map.of(
                "op", op,
                "path", path,
                "value", value
        );
    }
}
