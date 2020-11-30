package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import uk.gov.pay.commons.model.SupportedLanguage;

import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

class ServiceResourceCreateIT extends IntegrationTest {

    @Test
    void shouldCreateService() {
        JsonNode payload = mapper
                .valueToTree(Map.of(
                        "service_name", Map.of(SupportedLanguage.ENGLISH.toString(), "Service name"),
                        "gateway_account_ids", List.of("1")
                ));
        
        givenSetup()
                .when()
                .contentType(JSON)
                .body(payload)
                .post("v1/api/services")
                .then()
                .statusCode(201)
                .body("created_date", is(not(nullValue())))
                .body("gateway_account_ids", is(List.of("1")))
                .body("service_name", hasEntry("en", "Service name"));
    }
}
