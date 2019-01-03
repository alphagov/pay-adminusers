package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ServiceUpdateOperationValidatorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ServiceUpdateOperationValidator serviceUpdateOperationValidator = new ServiceUpdateOperationValidator(new RequestValidations());

    @Test
    public void shouldSuccess_whenUpdateName_withAllFieldsPresentAndValid() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "name", "op", "replace", "value", "example-name");

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void shouldFail_whenUpdateName_whenNameFieldPresentAndItIsTooLong() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "name", "op", "replace",
                "value", RandomStringUtils.randomAlphanumeric(51));

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [value] must have a maximum length of 50 characters"));
    }

    @Test
    public void shouldFail_whenUpdate_whenMissingRequiredField() {
        ImmutableMap<String, String> payload = ImmutableMap.of("value", "example-name");

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.size(), is(2));
        assertThat(errors, hasItem("Field [path] is required"));
        assertThat(errors, hasItem("Field [op] is required"));
    }

    @Test
    public void shouldFail_whenUpdate_whenInvalidPath() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "xyz", "op", "replace", "value", "example-name");

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Path [xyz] is invalid"));
    }

    @Test
    public void shouldFail_whenUpdate_whenInvalidOperationForSuppliedPath() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "name", "op", "add", "value", "example-name");

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Operation [add] is invalid for path [name]"));
    }

    @Test
    public void shouldSuccess_replacingCustomBranding() {
        ImmutableMap<String, Object> payload = ImmutableMap.of("path", "custom_branding", "op", "replace",
                "value", ImmutableMap.of("image_url", "image url", "css_url", "css url"));

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void shouldSuccess_replacingCustomBranding_forEmptyObject() {
        ImmutableMap<String, Object> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", ImmutableMap.of());

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void shouldError_ifCustomBrandingIsEmptyString() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", "");

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Value for path [custom_branding] must be a JSON"));
    }

    @Test
    public void shouldError_ifCustomBrandingIsNull() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "custom_branding", "op", "replace");

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Value for path [custom_branding] must be a JSON"));
    }

    @Test
    public void shouldError_replacingCustomBranding_ifValueIsNotJSON() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", "&*£&^(P%£");

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Value for path [custom_branding] must be a JSON"));
    }

    @Test
    public void shouldSuccess_whenUpdateServiceName_withAllFieldsPresentAndValid() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "service_name/en", "op", "replace", "value", "example-name");

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void shouldSuccess_whenUpdateServiceName_withAllFieldsPresentAndWelshServiceNameBlank() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "service_name/cy", "op", "replace", "value", " ");

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void shouldFail_whenUpdateServiceName_withAllFieldsPresentAndEnglishServiceNameBlank() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "service_name/en", "op", "replace", "value", "");

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [value] is required"));
    }

    @Test
    public void shouldFail_whenUpdateServiceName_whenServiceNameFieldPresentAndItIsTooLong() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "service_name/en", "op", "replace",
                "value", RandomStringUtils.randomAlphanumeric(51));

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [value] must have a maximum length of 50 characters"));
    }

    @Test
    public void shouldFail_whenUpdateServiceName_whenPathContainsUnsupportedLanguage() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "service_name/xx", "op", "replace", "value", "example-name");

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Path [service_name/xx] is invalid"));
    }

    @Test
    public void shouldSucceed_whenUpdateRedirectToService() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "redirect_to_service_immediately_on_terminal_state");
        payload.put("op", "replace");
        payload.put("value", true);

        List<String> errors = serviceUpdateOperationValidator.validate(payload);

        assertThat(errors.size(), is(0));
    }

    @Test
    public void shouldFail_whenUpdateRedirectToService_whenOpIsNotReplace() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "redirect_to_service_immediately_on_terminal_state");
        payload.put("op", "not_replace");
        payload.put("value", true);

        List<String> errors = serviceUpdateOperationValidator.validate(payload);

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Operation [not_replace] is invalid for path [redirect_to_service_immediately_on_terminal_state]"));
    }

    @Test
    public void shouldFail_whenUpdateRedirectToService_whenOpIsNotPresent() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "redirect_to_service_immediately_on_terminal_state");
        payload.put("value", true);

        List<String> errors = serviceUpdateOperationValidator.validate(payload);

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [op] is required"));
    }

    @Test
    public void shouldFail_whenUpdateRedirectToService_whenValueIsNotBoolean() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "redirect_to_service_immediately_on_terminal_state");
        payload.put("op", "replace");
        payload.put("value", "not a boolean");

        List<String> errors = serviceUpdateOperationValidator.validate(payload);

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [value] must be a boolean"));
    }

    @Test
    public void shouldFail_whenUpdateRedirectToService_whenMissingValue() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "redirect_to_service_immediately_on_terminal_state");
        payload.put("op", "replace");

        List<String> errors = serviceUpdateOperationValidator.validate(payload);

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [value] is required"));
    }

    @Test
    public void shouldSucceed_whenUpdateCollectBillingAddress() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "collect_billing_address");
        payload.put("op", "replace");
        payload.put("value", true);

        List<String> errors = serviceUpdateOperationValidator.validate(payload);

        assertThat(errors.size(), is(0));
    }

    @Test
    public void shouldFail_whenUpdateCollectBillingAddress_whenOpIsNotReplace() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "collect_billing_address");
        payload.put("op", "not_replace");
        payload.put("value", true);

        List<String> errors = serviceUpdateOperationValidator.validate(payload);

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Operation [not_replace] is invalid for path [collect_billing_address]"));
    }

    @Test
    public void shouldFail_whenUpdateCollectBillingAddress_whenOpIsNotPresent() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "collect_billing_address");
        payload.put("value", true);

        List<String> errors = serviceUpdateOperationValidator.validate(payload);

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [op] is required"));
    }

    @Test
    public void shouldFail_whenUpdateCollectBillingAddress_whenValueIsNotBoolean() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "collect_billing_address");
        payload.put("op", "replace");
        payload.put("value", "not a boolean");

        List<String> errors = serviceUpdateOperationValidator.validate(payload);

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [value] must be a boolean"));
    }

    @Test
    public void shouldFail_whenUpdateCollectBillingAddress_whenMissingValue() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "collect_billing_address");
        payload.put("op", "replace");

        List<String> errors = serviceUpdateOperationValidator.validate(payload);

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [value] is required"));
    }

    @Test
    public void shouldFail_updatingServiceName_whenValueIsNumeric() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "name");
        payload.put("op", "replace");
        payload.put("value", 42);
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [value] must be a string"));
    }

    @Test
    public void shouldFail_updatingServiceName_whenValueIsBoolean() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "service_name/en");
        payload.put("op", "replace");
        payload.put("value", false);
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [value] must be a string"));
    }

    @Test
    public void shouldFail_updatingCurrentGoLiveStage_whenInvalidValue() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "current_go_live_stage");
        payload.put("op", "replace");
        payload.put("value", "CAKE_ORDERED");
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [value] must be one of [NOT_STARTED, ENTERED_ORGANISATION_NAME, CHOSEN_PSP_STRIPE, CHOSEN_PSP_WORLDPAY, CHOSEN_PSP_SMARTPAY, CHOSEN_PSP_EPDQ, TERMS_AGREED_STRIPE, TERMS_AGREED_WORLDPAY, TERMS_AGREED_SMARTPAY, TERMS_AGREED_EPDQ, DENIED, LIVE]"));
    }

    @Test
    public void shouldFail_updatingCurrentGoLiveStage_whenIncorrectOperation() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "current_go_live_stage");
        payload.put("op", "add");
        payload.put("value", "CHOSEN_PSP_STRIPE");

        List<String> errors = serviceUpdateOperationValidator.validate(payload);

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Operation [add] is invalid for path [current_go_live_stage]"));
    }

    @Test
    public void shouldFail_updatingCurrentGoLiveStage_whenOperationIsMissing() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "current_go_live_stage");
        payload.put("value", "CHOSEN_PSP_STRIPE");

        List<String> errors = serviceUpdateOperationValidator.validate(payload);

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [op] is required"));
    }

    @Test
    public void shouldFail_updatingCurrentGoLiveStag_whenValueIsNumeric() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "current_go_live_stage");
        payload.put("op", "replace");
        payload.put("value", 42);
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [value] must be one of [NOT_STARTED, ENTERED_ORGANISATION_NAME, CHOSEN_PSP_STRIPE, CHOSEN_PSP_WORLDPAY, CHOSEN_PSP_SMARTPAY, CHOSEN_PSP_EPDQ, TERMS_AGREED_STRIPE, TERMS_AGREED_WORLDPAY, TERMS_AGREED_SMARTPAY, TERMS_AGREED_EPDQ, DENIED, LIVE]"));
    }

    @Test
    public void shouldFail_updatingCurrentGoLiveStag_whenValueIsBoolean() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "current_go_live_stage");
        payload.put("op", "replace");
        payload.put("value", false);
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [value] must be one of [NOT_STARTED, ENTERED_ORGANISATION_NAME, CHOSEN_PSP_STRIPE, CHOSEN_PSP_WORLDPAY, CHOSEN_PSP_SMARTPAY, CHOSEN_PSP_EPDQ, TERMS_AGREED_STRIPE, TERMS_AGREED_WORLDPAY, TERMS_AGREED_SMARTPAY, TERMS_AGREED_EPDQ, DENIED, LIVE]"));
    }

    @Test
    public void shouldFail_updatingCurrentGoLiveStage_whenValueIsMissing() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "current_go_live_stage");
        payload.put("op", "replace");
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [value] is required"));
    }

    @Test
    public void shouldFail_updatingCurrentGoLiveStage_whenValueIsEmptyString() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "current_go_live_stage");
        payload.put("op", "replace");
        payload.put("value", "");
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [value] is required"));
    }

    @Test
    public void shouldSucceed_updatingCurrentGoLiveStage() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "current_go_live_stage");
        payload.put("op", "replace");
        payload.put("value", "CHOSEN_PSP_STRIPE");
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(0));
    }
}
