package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import uk.gov.pay.adminusers.exception.ValidationException;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServiceRequestValidatorTest {

    private ObjectMapper mapper = new ObjectMapper();
    private ServiceRequestValidator serviceRequestValidator = new ServiceRequestValidator(new RequestValidations());

    @Test
    public void shouldSuccess_whenUpdate_whenAllFieldPresentAndValid_andJsonNotArray() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "name", "op", "replace", "value", "example-name");

        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));
        assertFalse(errors.isPresent());
    }

    @Test
    public void shouldSuccess_whenUpdateArray_withAllFieldsPresentAndValid() throws IOException {
        ImmutableMap<String, Object> op1 = ImmutableMap.of("path", "name", "op", "replace", "value", "new-en-name");
        ImmutableMap<String, Object> op2 = ImmutableMap.of(
                "path", "service_name",
                "op", "replace",
                "value", ImmutableMap.of("cy", "new-cy-name"));
        List<Map> payload = Arrays.asList(op1, op2);

        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));
        assertFalse(errors.isPresent());
    }

    @Test
    public void shouldAllowNonNumericGatewayAccounts_whenFindingServices() {
        Optional<Errors> errors = serviceRequestValidator.validateFindRequest("non-numeric-id");
        assertThat(errors.isPresent(), is(false));
    }

    @Test
    public void shouldFail_whenUpdate_whenServiceNameFieldPresentAndItIsTooLong() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "name", "op", "replace", "value", RandomStringUtils.randomAlphanumeric(51));

        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertTrue(errors.isPresent());
        List<String> errorsList = errors.get().getErrors();
        assertThat(errorsList.size(), is(1));
        assertThat(errorsList, hasItem("Field [value] must have a maximum length of 50 characters"));
    }

    @Test
    public void shouldFail_whenUpdate_whenMissingRequiredField() {
        ImmutableMap<String, String> payload = ImmutableMap.of("value", "example-name");

        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertTrue(errors.isPresent());
        List<String> errorsList = errors.get().getErrors();
        assertThat(errorsList.size(), is(2));
        assertThat(errorsList, hasItem("Field [path] is required"));
        assertThat(errorsList, hasItem("Field [op] is required"));
    }

    @Test
    public void shouldFail_whenUpdate_whenInvalidPath() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "xyz", "op", "replace", "value", "example-name");

        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertTrue(errors.isPresent());
        List<String> errorsList = errors.get().getErrors();
        assertThat(errorsList.size(), is(1));
        assertThat(errorsList, hasItem("Path [xyz] is invalid"));
    }

    @Test
    public void shouldFail_whenUpdate_whenInvalidOperationForSuppliedPath() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "name", "op", "add", "value", "example-name");

        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertTrue(errors.isPresent());
        List<String> errorsList = errors.get().getErrors();
        assertThat(errorsList.size(), is(1));
        assertThat(errorsList, hasItem("Operation [add] is invalid for path [name]"));
    }

    @Test
    public void shouldSuccess_updatingMerchantDetails() throws ValidationException {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_NAME, "name");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_LINE1, "line1");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_CITY, "city");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_COUNTRY, "country");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE, "postcode");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_EMAIL, "dd-merchant@example.com");

        serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload);
    }

    @Test(expected = ValidationException.class)
    public void shouldFail_updatingMerchantDetails_forEmptyObject() throws ValidationException {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload);
    }

    @Test(expected = ValidationException.class)
    public void shouldFail_updatingMerchantDetails_forMissingMandatoryFields() throws ValidationException {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_LINE1, "line1");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_CITY, "city");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_COUNTRY, "country");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE, "postcode");

        serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload);
    }

    @Test(expected = ValidationException.class)
    public void shouldFail_updatingMerchantDetails_forBlankStringMandatoryFields() throws ValidationException {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_NAME, "");

        serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload);
    }

    @Test(expected = ValidationException.class)
    public void shouldFail_updatingMerchantDetails_forNullValueMandatoryFields() throws ValidationException {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.set(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_NAME, null);

        serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload);
    }

    @Test
    public void shouldFail_updatingMerchantDetails_whenInvalidEmail() {
        ObjectNode payload = createJsonPayload("invalid@example.com-uk");

        try {
            serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload);
        } catch (ValidationException e) {
            assertThat(e.getErrors().getErrors(), hasItem("Field [email] must be a valid email address"));
        }
    }

    @Test
    public void shouldFail_updatingMerchantDetails_whenEmailOver255() {
        String longEmail = RandomStringUtils.randomAlphanumeric(256);
        ObjectNode payload = createJsonPayload(longEmail);

        try {
            serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload);
        } catch (ValidationException e) {
            assertThat(e.getErrors().getErrors(), hasItem("Field [email] must have a maximum length of 255 characters"));
        }
    }

    @Test
    public void shouldSuccess_replacingCustomBranding() {
        ImmutableMap<String, Object> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", ImmutableMap.of("image_url", "image url", "css_url", "css url"));
        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertThat(errors.isPresent(), is(false));
    }

    @Test
    public void shouldSuccess_replacingCustomBranding_forEmptyObject() {
        ImmutableMap<String, Object> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", ImmutableMap.of());
        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertThat(errors.isPresent(), is(false));
    }

    @Test
    public void shouldError_ifCustomBrandingIsEmptyString() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", "");
        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertThat(errors.isPresent(), is(true));
        List<String> errorsList = errors.get().getErrors();
        assertThat(errorsList.size(), is(1));
        assertThat(errorsList, hasItem("Value for path [custom_branding] must be a JSON"));
    }

    @Test
    public void shouldError_ifCustomBrandingIsNull() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "custom_branding", "op", "replace");
        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertThat(errors.isPresent(), is(true));
        List<String> errorsList = errors.get().getErrors();
        assertThat(errorsList.size(), is(1));
        assertThat(errorsList, hasItem("Value for path [custom_branding] must be a JSON"));
    }

    @Test
    public void shouldError_replacingCustomBranding_ifValueIsNotJSON() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", "&*£&^(P%£");
        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertThat(errors.isPresent(), is(true));
        List<String> errorsList = errors.get().getErrors();
        assertThat(errorsList.size(), is(1));
        assertThat(errorsList, hasItem("Value for path [custom_branding] must be a JSON"));
    }

    private ObjectNode createJsonPayload(String email) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_NAME, "Merchant name");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_LINE1, "line1");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_CITY, "city");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_COUNTRY, "country");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE, "postcode");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_EMAIL, email);
        return payload;
    }
}
