package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ServiceUpdateOperationValidatorTest {

    private static final String GO_LIVE_STAGE_INVALID_ERROR_MESSAGE = "Field [value] must be one of [NOT_STARTED, ENTERED_ORGANISATION_NAME, ENTERED_ORGANISATION_ADDRESS, CHOSEN_PSP_STRIPE, CHOSEN_PSP_WORLDPAY, CHOSEN_PSP_SMARTPAY, CHOSEN_PSP_EPDQ, CHOSEN_PSP_GOV_BANKING_WORLDPAY, TERMS_AGREED_STRIPE, TERMS_AGREED_WORLDPAY, TERMS_AGREED_SMARTPAY, TERMS_AGREED_EPDQ, TERMS_AGREED_GOV_BANKING_WORLDPAY, DENIED, LIVE]";
    private static final String PSP_TEST_ACCOUNT_STAGE_INVALID_ERROR_MESSAGE = "Field [value] must be one of [NOT_STARTED, REQUEST_SUBMITTED, CREATED]";

    private final ObjectMapper mapper = new ObjectMapper();
    private final ServiceUpdateOperationValidator serviceUpdateOperationValidator = new ServiceUpdateOperationValidator(new RequestValidations());

    @Test
    public void shouldFail_whenUpdate_whenMissingRequiredField() {
        Map<String, String> payload = Map.of("value", "example-name");

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.size(), is(2));
        assertThat(errors, hasItem("Field [path] is required"));
        assertThat(errors, hasItem("Field [op] is required"));
    }

    @Test
    public void shouldFail_whenUpdate_whenInvalidPath() {
        shouldFail("xyz", "replace", "any value", "Path [xyz] is invalid");
    }

    @Test
    public void shouldFail_whenUpdateServiceName_whenPathContainsUnsupportedLanguage() {
        shouldFail("service_name/xx", "replace", "example name", "Path [service_name/xx] is invalid");
    }

    private static Object[] shouldFailForOperationParams() {
        return new Object[]{
                new Object[]{"replace", "gateway_account_ids", List.of(1, 2)},
                new Object[]{"add", "collect_billing_address", false},
                new Object[]{"add", "experimental_features_enabled", true},
                new Object[]{"add", "redirect_to_service_immediately_on_terminal_state", true},
                new Object[]{"add", "collect_billing_address", true},
                new Object[]{"add", "current_go_live_stage", "CHOSEN_PSP_STRIPE"},
                new Object[]{"add", "current_psp_test_account_stage", "REQUEST_SUBMITTED"},
                new Object[]{"add", "experimental_features_enabled", false},
                new Object[]{"add", "agent_initiated_moto_enabled", false},
                new Object[]{"add", "sector", "any value"},
                new Object[]{"add", "internal", false},
                new Object[]{"add", "archived", false},
                new Object[]{"add", "went_live_date", "2020-01-01T01:01:00Z"},
                new Object[]{"add", "merchant_details/name", "any value"},
                new Object[]{"add", "merchant_details/address_line1", "any value"},
                new Object[]{"add", "merchant_details/address_line2", "any value"},
                new Object[]{"add", "merchant_details/address_city", "any value"},
                new Object[]{"add", "merchant_details/address_country", "any value"},
                new Object[]{"add", "merchant_details/address_postcode", "any value"},
                new Object[]{"add", "merchant_details/email", "any value"},
                new Object[]{"add", "merchant_details/telephone_number", "any value"}
        };
    }

    @ParameterizedTest
    @MethodSource("shouldFailForOperationParams")
    public void shouldFailForOperation(String operation, String path, Object value) {
        String expectedErrorMessage = String.format("Operation [%s] is invalid for path [%s]", operation, path);
        shouldFail(path, operation, value, expectedErrorMessage);
    }

    private static Object[] replaceShouldFailWhenValueInvalidValueParameters() {
        return new Object[]{
                new Object[]{"service_name/en", 42, "Field [value] must be a string"},
                new Object[]{"sector", 42, "Field [value] must be a string"},
                new Object[]{"current_go_live_stage", 42, GO_LIVE_STAGE_INVALID_ERROR_MESSAGE},
                new Object[]{"current_go_live_stage", "CAKE_ORDERED", GO_LIVE_STAGE_INVALID_ERROR_MESSAGE},
                new Object[]{"current_psp_test_account_stage", "42", PSP_TEST_ACCOUNT_STAGE_INVALID_ERROR_MESSAGE},
                new Object[]{"merchant_details/name", 42, "Field [value] must be a string"},
                new Object[]{"merchant_details/address_line1", 42, "Field [value] must be a string"},
                new Object[]{"merchant_details/address_line2", 42, "Field [value] must be a string"},
                new Object[]{"merchant_details/address_city", 42, "Field [value] must be a string"},
                new Object[]{"merchant_details/address_country", 42, "Field [value] must be a string"},
                new Object[]{"merchant_details/address_postcode", 42, "Field [value] must be a string"},
                new Object[]{"merchant_details/email", 42, "Field [value] must be a string"},
                new Object[]{"merchant_details/telephone_number", 42, "Field [value] must be a string"},
                new Object[]{"collect_billing_address", "a string", "Field [value] must be a boolean"},
                new Object[]{"redirect_to_service_immediately_on_terminal_state", "a string", "Field [value] must be a boolean"},
                new Object[]{"experimental_features_enabled", "a string", "Field [value] must be a boolean"},
                new Object[]{"agent_initiated_moto_enabled", 42, "Field [value] must be a boolean"},
                new Object[]{"internal", "a string", "Field [value] must be a boolean"},
                new Object[]{"archived", "a string", "Field [value] must be a boolean"},
                new Object[]{"custom_branding", "a string", "Value for path [custom_branding] must be a JSON"},
                new Object[]{"went_live_date", 42, "Field [value] must be a valid date time with timezone"},
                new Object[]{"went_live_date", "a string", "Field [value] must be a valid date time with timezone"},
        };
    }

    @ParameterizedTest
    @MethodSource("replaceShouldFailWhenValueInvalidValueParameters")
    public void replaceShouldFailWhenInvalidValue(String path, Object value, String expectedErrorMessage) {
        shouldFail(path, "replace", value, expectedErrorMessage);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "redirect_to_service_immediately_on_terminal_state",
            "collect_billing_address",
            "current_go_live_stage",
            "current_psp_test_account_stage",
            "experimental_features_enabled",
            "agent_initiated_moto_enabled",
            "merchant_details/name",
            "merchant_details/address_line1",
            "merchant_details/address_line2",
            "merchant_details/address_city",
            "merchant_details/address_country",
            "merchant_details/address_postcode",
            "merchant_details/email",
            "merchant_details/telephone_number",
            "custom_branding",
            "sector",
            "internal",
            "archived",
            "went_live_date"
    })
    public void replaceShouldFailWhenValueMissing(String path) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", path);
        payload.put("op", "replace");
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [value] is required"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "redirect_to_service_immediately_on_terminal_state",
            "collect_billing_address",
            "current_go_live_stage",
            "current_psp_test_account_stage",
            "experimental_features_enabled",
            "agent_initiated_moto_enabled",
            "merchant_details/name",
            "merchant_details/address_line1",
            "merchant_details/address_line2",
            "merchant_details/address_city",
            "merchant_details/address_country",
            "merchant_details/address_postcode",
            "merchant_details/email",
            "merchant_details/telephone_number",
            "custom_branding",
            "sector",
            "internal",
            "archived",
            "went_live_date"
    })
    public void replaceShouldFailWhenValueIsNull(String path) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", path);
        payload.put("op", "replace");
        payload.putNull("value");
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [value] is required"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "service_name/en",
            "current_go_live_stage",
            "current_psp_test_account_stage",
            "merchant_details/name",
            "merchant_details/address_line1",
            "merchant_details/address_city",
            "merchant_details/address_country",
            "merchant_details/address_postcode",
    })
    public void replaceShouldFailWhenValueIsEmptyString(String path) {
        shouldFail(path, "replace", "", "Field [value] is required");
    }

    private static Object[] shouldFailWhenStringValueIsTooLongParameters() {
        return new Object[]{
                new Object[]{"service_name/en", 50},
                new Object[]{"merchant_details/name", 255},
                new Object[]{"merchant_details/address_line1", 255},
                new Object[]{"merchant_details/address_line2", 255},
                new Object[]{"merchant_details/address_city", 255},
                new Object[]{"merchant_details/address_country", 10},
                new Object[]{"merchant_details/address_postcode", 25},
                new Object[]{"merchant_details/email", 255},
                new Object[]{"merchant_details/telephone_number", 255},
                new Object[]{"sector", 50}
        };
    }

    @ParameterizedTest
    @MethodSource("shouldFailWhenStringValueIsTooLongParameters")
    public void replaceShouldFailWhenStringValueIsTooLong(String path, int expectedMaxLength) {
        shouldFail(path, "replace", randomAlphanumeric(expectedMaxLength + 1), String.format("Field [value] must have a maximum length of %s characters", expectedMaxLength));
    }

    private static Object[] shouldSucceedParams() {
        return new Object[]{
                new Object[]{"add", "gateway_account_ids", List.of(1, 2)},
                new Object[]{"replace", "redirect_to_service_immediately_on_terminal_state", true},
                new Object[]{"replace", "experimental_features_enabled", true},
                new Object[]{"replace", "agent_initiated_moto_enabled", true},
                new Object[]{"replace", "collect_billing_address", true},
                new Object[]{"replace", "current_go_live_stage", "CHOSEN_PSP_STRIPE"},
                new Object[]{"replace", "current_psp_test_account_stage", "REQUEST_SUBMITTED"},
                new Object[]{"replace", "merchant_details/name", "Bob's Building Business"},
                new Object[]{"replace", "merchant_details/address_line1", "1 Builders Avenue"},
                new Object[]{"replace", "merchant_details/address_line2", ""},
                new Object[]{"replace", "merchant_details/address_city", "Burnley"},
                new Object[]{"replace", "merchant_details/address_country", "GB"},
                new Object[]{"replace", "merchant_details/address_postcode", "B52 9EG"},
                new Object[]{"replace", "merchant_details/email", ""},
                new Object[]{"replace", "merchant_details/email", "bob-the-builder@example.com"},
                new Object[]{"replace", "merchant_details/telephone_number", ""},
                new Object[]{"replace", "merchant_details/telephone_number", "00000000000"},
                new Object[]{"replace", "custom_branding", Map.of("image_url", "image url", "css_url", "css url")},
                new Object[]{"replace", "custom_branding", emptyMap()},
                new Object[]{"replace", "service_name/en", "example-name"},
                new Object[]{"replace", "sector", "local government"},
                new Object[]{"replace", "internal", true},
                new Object[]{"replace", "archived", true},
                new Object[]{"replace", "went_live_date", "2020-01-01T01:01:00Z"}

        };
    }

    @ParameterizedTest
    @MethodSource("shouldSucceedParams")
    public void shouldSucceed(String operation, String path, Object value) {
        Map<String, Object> payload = Map.of(
                "path", path,
                "op", operation,
                "value", value);

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));
        assertThat(errors.size(), is(0));
    }

    private void shouldFail(String path, String operation, Object value, String expectedErrorMessage) {
        ObjectNode payload = mapper.valueToTree(Map.of(
                "path", path,
                "op", operation,
                "value", value
        ));
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem(expectedErrorMessage));
    }
}
