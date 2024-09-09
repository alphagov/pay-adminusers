package uk.gov.pay.adminusers.resources;

import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.service.payments.commons.model.SupportedLanguage.ENGLISH;
import static uk.gov.service.payments.commons.model.SupportedLanguage.WELSH;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ServiceResourceCreateIT extends IntegrationTest {

    @Test
    void create_service_with_all_parameters_successfully() {
        var validatableResponse = givenSetup()
                .when()
                .contentType(JSON)
                .body(Map.of("service_name", Map.of(ENGLISH.toString(), "Service name"),
                        "gateway_account_ids", List.of("1")))
                .post("v1/api/services")
                .then()
                .statusCode(201)
                .body("created_date", is(not(nullValue())))
                .body("gateway_account_ids", is(List.of("1")))
                .body("service_name", hasEntry("en", "Service name"));
        
        assertStandardFields(validatableResponse);
    }

    @Test
    void can_create_default_service_with_empty_request_body() {
        var validatableResponse = givenSetup()
                .when()
                .contentType(JSON)
                .post("v1/api/services")
                .then()
                .statusCode(201)
                .body("created_date", is(not(nullValue())))
                .body("gateway_account_ids", is(emptyIterable()))
                .body("service_name", hasEntry("en", "System Generated"))
                .body("name", is("System Generated"));
        
        assertStandardFields(validatableResponse);
    }
    
    @Test
    void can_create_default_service_with_gateway_account_ids_only() {
        var validatableResponse = givenSetup()
                .when()
                .contentType(JSON)
                .body(Map.of("gateway_account_ids", List.of("1", "2")))
                .post("v1/api/services")
                .then()
                .statusCode(201)
                .body("created_date", is(not(nullValue())))
                .body("gateway_account_ids", is(List.of("1", "2")))
                .body("service_name", hasEntry("en", "System Generated"))
                .body("name", is("System Generated"));

        assertStandardFields(validatableResponse);
    }

    @Test
    void can_create_default_service_with_service_name_only() {
        var validatableResponse = givenSetup()
                .when()
                .contentType(JSON)
                .body(Map.of("service_name", Map.of(ENGLISH.toString(), "Service name", WELSH.toString(), "Welsh name")))
                .post("v1/api/services")
                .then()
                .statusCode(201)
                .body("created_date", is(not(nullValue())))
                .body("gateway_account_ids", is(emptyIterable()))
                .body("service_name", hasEntry("en", "Service name"))
                .body("service_name", hasEntry("cy", "Welsh name"))
                .body("name", is("Service name"));

        assertStandardFields(validatableResponse);
    }

    private void assertStandardFields(ValidatableResponse validatableResponse) {
        validatableResponse
                .body("current_psp_test_account_stage", is("NOT_STARTED"))
                .body("current_go_live_stage", is("NOT_STARTED"))
                .body("default_billing_address_country", is("GB"))
                .body("agent_initiated_moto_enabled", is(false))
                .body("takes_payments_over_phone", is(false))
                .body("experimental_features_enabled", is(false))
                .body("internal", is(false))
                .body("archived", is(false))
                .body("redirect_to_service_immediately_on_terminal_state", is(false))
                .body("collect_billing_address", is(true));
    }
    
    @Test
    void return_bad_request_when_invalid_supported_language_provided() {
        givenSetup()
                .when()
                .contentType(JSON)
                .body(Map.of("service_name", Map.of("fr", "Service name")))
                .post("v1/api/services")
                .then()
                .statusCode(400)
                .body("message", is("Unable to process JSON"))
                .body("details", is("fr is not a supported ISO 639-1 code"));
    }
}
