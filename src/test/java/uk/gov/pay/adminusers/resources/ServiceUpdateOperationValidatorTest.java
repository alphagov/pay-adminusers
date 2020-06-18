package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ServiceUpdateOperationValidatorTest {

    private static final String GO_LIVE_STAGE_INVALID_ERROR_MESSAGE = "Field [value] must be one of [NOT_STARTED, ENTERED_ORGANISATION_NAME, ENTERED_ORGANISATION_ADDRESS, CHOSEN_PSP_STRIPE, CHOSEN_PSP_WORLDPAY, CHOSEN_PSP_SMARTPAY, CHOSEN_PSP_EPDQ, TERMS_AGREED_STRIPE, TERMS_AGREED_WORLDPAY, TERMS_AGREED_SMARTPAY, TERMS_AGREED_EPDQ, DENIED, LIVE]";
    
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
        Map<String, String> payload = Map.of(
                "path", "xyz",
                "op", "replace",
                "value", "example-name");

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Path [xyz] is invalid"));
    }

    @Test
    public void shouldFail_whenUpdate_whenInvalidOperationForSuppliedPath() {
        shouldFailForAddOperation("collect_billing_address", false);
    }

    @Test
    public void shouldSuccess_replacingCustomBranding() {
        Map<String, String> branding = Map.of("image_url", "image url", "css_url", "css url");
        replaceShouldSucceed("custom_branding", branding);
    }

    @Test
    public void shouldSuccess_replacingCustomBranding_forEmptyObject() {
        replaceShouldSucceed("custom_branding", emptyMap());
    }

    @Test
    public void shouldError_ifCustomBrandingIsEmptyString() {
        replaceShouldFailWhenValueIsEmptyString("custom_branding", "Value for path [custom_branding] must be a JSON");
    }

    @Test
    public void shouldError_ifCustomBrandingIsNull() {
        replaceShouldFailWhenValueIsNull("custom_branding", "Value for path [custom_branding] must be a JSON");
    }

    @Test
    public void shouldError_replacingCustomBranding_ifValueIsNotJSON() {
        replaceShouldFailWhenValueIsString("custom_branding", "Value for path [custom_branding] must be a JSON");
    }

    @Test
    public void shouldSuccess_whenUpdateServiceName_withAllFieldsPresentAndValid() {
        replaceShouldSucceed("service_name/en", "example-name");
    }

    @Test
    public void shouldSuccess_whenUpdateServiceName_withAllFieldsPresentAndWelshServiceNameBlank() {
        replaceShouldSucceed("service_name/cy", " ");
    }

    @Test
    public void shouldFail_whenUpdateServiceName_withAllFieldsPresentAndEnglishServiceNameBlank() {
        replaceShouldFailWhenValueIsEmptyString("service_name/en");
    }

    @Test
    public void shouldFail_whenUpdateServiceName_whenServiceNameFieldPresentAndItIsTooLong() {
        replaceShouldFailWhenStringValueIsTooLong("service_name/en", 50);
    }

    @Test
    public void shouldFail_whenUpdateServiceName_whenPathContainsUnsupportedLanguage() {
        Map<String, String> payload = Map.of(
                "path", "service_name/xx",
                "op", "replace",
                "value", "example-name");

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Path [service_name/xx] is invalid"));
    }

    @Test
    public void shouldSucceed_whenUpdateRedirectToService() {
        replaceShouldSucceed("redirect_to_service_immediately_on_terminal_state", true);
    }

    @Test
    public void shouldSucceed_whenUpdateExperimentalFeaturesEnabled() {
        replaceShouldSucceed("experimental_features_enabled", true);
    }

    @Test
    public void shouldFail_whenUpdateExperimentalFeaturesEnabled_whenOpIsNotReplace() {
        shouldFailForAddOperation("experimental_features_enabled", true);
    }

    @Test
    public void shouldFail_whenUpdateRedirectToService_whenOpIsNotReplace() {
        shouldFailForAddOperation("redirect_to_service_immediately_on_terminal_state", true);
    }

    @Test
    public void shouldFail_whenUpdateRedirectToService_whenValueIsNotBoolean() {
        replaceShouldFailWhenValueIsString("redirect_to_service_immediately_on_terminal_state", "Field [value] must be a boolean");
    }

    @Test
    public void shouldFail_whenUpdateRedirectToService_whenMissingValue() {
        replaceShouldFailWhenValueMissing("redirect_to_service_immediately_on_terminal_state");
    }

    @Test
    public void shouldSucceed_whenUpdateCollectBillingAddress() {
        replaceShouldSucceed("collect_billing_address", true);
    }

    @Test
    public void shouldFail_whenUpdateCollectBillingAddress_whenOpIsNotReplace() {
        shouldFailForAddOperation("collect_billing_address", true);
    }

    @Test
    public void shouldFail_whenUpdateCollectBillingAddress_whenValueIsNotBoolean() {
        replaceShouldFailWhenValueIsString("collect_billing_address", "Field [value] must be a boolean");
    }

    @Test
    public void shouldFail_whenUpdateCollectBillingAddress_whenMissingValue() {
        replaceShouldFailWhenValueMissing("collect_billing_address");
    }

    @Test
    public void shouldFail_updatingServiceName_whenValueIsNumeric() {
        replaceShouldFailWhenValueIsNumeric("service_name/en", "Field [value] must be a string");
    }

    @Test
    public void shouldFail_updatingServiceName_whenValueIsBoolean() {
        replaceShouldFailWhenValueIsBoolean("service_name/en", "Field [value] must be a string");
    }

    @Test
    public void shouldFail_updatingCurrentGoLiveStage_whenInvalidValue() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", "current_go_live_stage");
        payload.put("op", "replace");
        payload.put("value", "CAKE_ORDERED");
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem(GO_LIVE_STAGE_INVALID_ERROR_MESSAGE));
    }

    @Test
    public void shouldFail_updatingCurrentGoLiveStage_whenIncorrectOperation() {
        shouldFailForAddOperation("current_go_live_stage", "CHOSEN_PSP_STRIPE");
    }

    @Test
    public void shouldFail_updatingCurrentGoLiveStage_whenValueIsNumeric() {
        replaceShouldFailWhenValueIsNumeric("current_go_live_stage", GO_LIVE_STAGE_INVALID_ERROR_MESSAGE);
    }

    @Test
    public void shouldFail_updatingCurrentGoLiveStage_whenValueIsBoolean() {
        replaceShouldFailWhenValueIsBoolean("current_go_live_stage", GO_LIVE_STAGE_INVALID_ERROR_MESSAGE);
    }

    @Test
    public void shouldFail_updatingCurrentGoLiveStage_whenValueIsMissing() {
        replaceShouldFailWhenValueMissing("current_go_live_stage");
    }

    @Test
    public void shouldFail_updatingCurrentGoLiveStage_whenValueIsEmptyString() {
        replaceShouldFailWhenValueIsEmptyString("current_go_live_stage");
    }

    @Test
    public void shouldSucceed_updatingCurrentGoLiveStage() {
        replaceShouldSucceed("current_go_live_stage", "CHOSEN_PSP_STRIPE");
    }
    
    @Test
    public void shouldFail_updatingMerchantDetailsName_whenNotAllowedOperation() {
        shouldFailForAddOperation("merchant_details/name", "any value");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsName_whenValueIsNotString() {
        replaceShouldFailWhenValueIsNumeric("merchant_details/name", "Field [value] must be a string");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsName_whenValueIsMissing() {
        replaceShouldFailWhenValueMissing("merchant_details/name");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsName_whenValueIsNull() {
        replaceShouldFailWhenValueIsNull("merchant_details/name");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsName_whenValueIsEmptyString() {
        replaceShouldFailWhenValueIsEmptyString("merchant_details/name");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsName_whenValueIsTooLong() {
        replaceShouldFailWhenStringValueIsTooLong("merchant_details/name", 255);
    }

    @Test
    public void shouldSucceed_updatingMerchantDetailsName() {
        replaceShouldSucceed("merchant_details/name", "Bob's Building Business");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressLine1_whenNotAllowedOperation() {
        shouldFailForAddOperation("merchant_details/address_line1", "any value");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressLine1_whenValueIsNotString() {
        replaceShouldFailWhenValueIsNumeric("merchant_details/address_line1", "Field [value] must be a string");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressLine1_whenValueIsMissing() {
        replaceShouldFailWhenValueMissing("merchant_details/address_line1");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressLine1_whenValueIsNull() {
        replaceShouldFailWhenValueIsNull("merchant_details/address_line1");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressLine1_whenValueIsEmptyString() {
        replaceShouldFailWhenValueIsEmptyString("merchant_details/address_line1");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressLine1_whenValueIsTooLong() {
        replaceShouldFailWhenStringValueIsTooLong("merchant_details/address_line1", 255);
    }

    @Test
    public void shouldSucceed_updatingMerchantDetailsAddressLine1() {
        replaceShouldSucceed("merchant_details/address_line1", "1 Builders Avenue");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressLine2_whenNotAllowedOperation() {
        shouldFailForAddOperation("merchant_details/address_line2", "any value");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressLine2_whenValueIsNotString() {
        replaceShouldFailWhenValueIsNumeric("merchant_details/address_line2", "Field [value] must be a string");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressLine2_whenValueIsMissing() {
        replaceShouldFailWhenValueMissing("merchant_details/address_line2");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressLine2_whenValueIsNull() {
        replaceShouldFailWhenValueIsNull("merchant_details/address_line2");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressLine2_whenValueIsTooLong() {
        replaceShouldFailWhenStringValueIsTooLong("merchant_details/address_line2", 255);
    }

    @Test
    public void shouldSucceed_updatingMerchantDetailsAddressLine2_whenValueIsEmptyString() {
        replaceShouldSucceed("merchant_details/address_line2", "");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressCity_whenNotAllowedOperation() {
        shouldFailForAddOperation("merchant_details/address_city", "any value");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressCity_whenValueIsNotString() {
        replaceShouldFailWhenValueIsNumeric("merchant_details/address_city", "Field [value] must be a string");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressCity_whenValueIsMissing() {
        replaceShouldFailWhenValueMissing("merchant_details/address_city");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressCity_whenValueIsNull() {
        replaceShouldFailWhenValueIsNull("merchant_details/address_city");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressCity_whenValueIsEmptyString() {
        replaceShouldFailWhenValueIsEmptyString("merchant_details/address_city");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressCity_whenValueIsTooLong() {
        replaceShouldFailWhenStringValueIsTooLong("merchant_details/address_city", 255);
    }

    @Test
    public void shouldSucceed_updatingMerchantDetailsAddressCity() {
        replaceShouldSucceed("merchant_details/address_city", "Burnley");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressCountry_whenNotAllowedOperation() {
        shouldFailForAddOperation("merchant_details/address_country", "any value");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressCountry_whenValueIsNotString() {
        replaceShouldFailWhenValueIsNumeric("merchant_details/address_country", "Field [value] must be a string");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressCountry_whenValueIsMissing() {
        replaceShouldFailWhenValueMissing("merchant_details/address_country");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressCountry_whenValueIsNull() {
        replaceShouldFailWhenValueIsNull("merchant_details/address_country");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressCountry_whenValueIsEmptyString() {
        replaceShouldFailWhenValueIsEmptyString("merchant_details/address_country");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressCountry_whenValueIsTooLong() {
        replaceShouldFailWhenStringValueIsTooLong("merchant_details/address_country", 10);
    }

    @Test
    public void shouldSucceed_updatingMerchantDetailsAddressCountry() {
        replaceShouldSucceed("merchant_details/address_country", "GB");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressPostcode_whenNotAllowedOperation() {
        shouldFailForAddOperation("merchant_details/address_postcode", "any value");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressPostcode_whenValueIsNotString() {
        replaceShouldFailWhenValueIsNumeric("merchant_details/address_postcode", "Field [value] must be a string");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressPostcode_whenValueIsMissing() {
        replaceShouldFailWhenValueMissing("merchant_details/address_postcode");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressPostcode_whenValueIsNull() {
        replaceShouldFailWhenValueIsNull("merchant_details/address_postcode");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressPostcode_whenValueIsEmptyString() {
        replaceShouldFailWhenValueIsEmptyString("merchant_details/address_postcode");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsAddressPostcode_whenValueIsTooLong() {
        replaceShouldFailWhenStringValueIsTooLong("merchant_details/address_postcode", 25);
    }

    @Test
    public void shouldSucceed_updatingMerchantDetailsAddressPostcode() {
        replaceShouldSucceed("merchant_details/address_postcode", "B52 9EG");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsEmail_whenNotAllowedOperation() {
        shouldFailForAddOperation("merchant_details/email", "any value");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsEmail_whenValueIsNotString() {
        replaceShouldFailWhenValueIsNumeric("merchant_details/email", "Field [value] must be a string");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsEmail_whenValueIsMissing() {
        replaceShouldFailWhenValueMissing("merchant_details/email");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsEmail_whenValueIsNull() {
        replaceShouldFailWhenValueIsNull("merchant_details/email");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsEmail_whenValueIsTooLong() {
        replaceShouldFailWhenStringValueIsTooLong("merchant_details/email", 255);
    }

    @Test
    public void shouldSucceed_updatingMerchantDetailsEmail_whenValueIsEmptyString() {
        replaceShouldSucceed("merchant_details/email", "");
    }

    @Test
    public void shouldSucceed_updatingMerchantDetailsEmail() {
        replaceShouldSucceed("merchant_details/email", "bob-the-builder@example.com");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsTelephoneNumber_whenNotAllowedOperation() {
        shouldFailForAddOperation("merchant_details/telephone_number", "any value");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsTelephoneNumber_whenValueIsNotString() {
        replaceShouldFailWhenValueIsNumeric("merchant_details/telephone_number", "Field [value] must be a string");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsTelephoneNumber_whenValueIsMissing() {
        replaceShouldFailWhenValueMissing("merchant_details/telephone_number");
    }

    @Test
    public void shouldFail_updatingMerchantDetailsTelephoneNumber_whenValueIsNull() {
        replaceShouldFailWhenValueIsNull("merchant_details/telephone_number");
    }
    

    @Test
    public void shouldFail_updatingMerchantDetailsTelephoneNumber_whenValueIsTooLong() {
        replaceShouldFailWhenStringValueIsTooLong("merchant_details/telephone_number", 255);
    }

    @Test
    public void shouldSucceed_updatingMerchantDetailsTelephoneNumber_whenValueIsEmptyString() {
        replaceShouldSucceed("merchant_details/telephone_number", "");
    }

    @Test
    public void shouldSucceed_updatingMerchantDetailsTelephoneNumber() {
        replaceShouldSucceed("merchant_details/telephone_number", "00000000000");
    }
    
    private void shouldFailForAddOperation(String path, Object value) {
        Map<String, Object> payload = Map.of(
                "path", path,
                "op", "add",
                "value", value);

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem(String.format("Operation [add] is invalid for path [%s]", path)));
    }

    private void replaceShouldFailWhenValueIsNumeric(String path, String expectedErrorMessage) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", path);
        payload.put("op", "replace");
        payload.put("value", 42);
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem(expectedErrorMessage));
    }

    private void replaceShouldFailWhenValueIsBoolean(String path, String expectedErrorMessage) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", path);
        payload.put("op", "replace");
        payload.put("value", false);
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem(expectedErrorMessage));
    }

    private void replaceShouldFailWhenValueIsString(String path, String expectedErrorMessage) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", path);
        payload.put("op", "replace");
        payload.put("value", "not a boolean");

        List<String> errors = serviceUpdateOperationValidator.validate(payload);

        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem(expectedErrorMessage));
    }

    private void replaceShouldFailWhenValueMissing(String path) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", path);
        payload.put("op", "replace");
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem("Field [value] is required"));
    }

    private void replaceShouldFailWhenValueIsNull(String path) {
        replaceShouldFailWhenValueIsNull(path, "Field [value] is required");
    }

    private void replaceShouldFailWhenValueIsNull(String path, String expectedErrorMessage) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", path);
        payload.put("op", "replace");
        payload.putNull("value");
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem(expectedErrorMessage));
    }

    private void replaceShouldFailWhenValueIsEmptyString(String path) {
        replaceShouldFailWhenValueIsEmptyString(path,"Field [value] is required");
    }

    private void replaceShouldFailWhenValueIsEmptyString(String path, String expectedErrorMessage) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", path);
        payload.put("op", "replace");
        payload.put("value", "");
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem(expectedErrorMessage));
    }

    private void replaceShouldFailWhenStringValueIsTooLong(String path, int expectedMaxLength) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("path", path);
        payload.put("op", "replace");
        payload.put("value", randomAlphanumeric(expectedMaxLength + 1));
        List<String> errors = serviceUpdateOperationValidator.validate(payload);
        assertThat(errors.size(), is(1));
        assertThat(errors, hasItem(String.format("Field [value] must have a maximum length of %s characters", expectedMaxLength)));
    }

    private void replaceShouldSucceed(String path, Object value) {
        Map<String, Object> payload = Map.of(
                "path", path,
                "op", "replace",
                "value", value);

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));
        assertThat(errors.size(), is(0));
    }
}
