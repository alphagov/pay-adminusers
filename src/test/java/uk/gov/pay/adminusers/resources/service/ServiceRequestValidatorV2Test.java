package uk.gov.pay.adminusers.resources.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.Test;
import uk.gov.pay.adminusers.exception.ValidationException;
import uk.gov.pay.adminusers.resources.ServiceRequestValidator;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ServiceRequestValidatorV2Test {
    private ObjectMapper mapper = new ObjectMapper();
    private ServiceRequestValidatorV2 serviceRequestValidatorV2 = new ServiceRequestValidatorV2(new RequestValidations());

    @Test
    public void shouldSuccess_whenUpdate_withAllFieldsPresentAndValid() throws IOException {

        String jsonString = fixture("fixtures/resource/service/patch/update-name-and-update-cy-name.json");

        Optional<Errors> errors = serviceRequestValidatorV2.validateUpdateAttributeRequest(mapper.readTree(jsonString));
        assertFalse(errors.isPresent());
    }
    
    @Test
    public void shouldThrowException_whenMissingOp() throws IOException {
        String jsonString = fixture("fixtures/resource/service/patch/update-name-missing-op.json");

        Optional<Errors> errors = serviceRequestValidatorV2.validateUpdateAttributeRequest(mapper.readTree(jsonString));
        assertThat(errors.isPresent(), is(true));
        assertThat(errors.get().getErrors(), hasSize(1));
        assertThat(errors.get().getErrors().get(0), is("Field [op] is required"));
    }

    @Test
    public void shouldThrowException_whenMissingValue() throws IOException {
        String jsonString = fixture("fixtures/resource/service/patch/update-name-missing-value.json");

        Optional<Errors> errors = serviceRequestValidatorV2.validateUpdateAttributeRequest(mapper.readTree(jsonString));
        assertThat(errors.isPresent(), is(true));
        assertThat(errors.get().getErrors(), hasSize(1));
        assertThat(errors.get().getErrors().get(0), is("Field [value] is required"));
    }

    @Test
    public void shouldThrowException_whenMissingPath() throws IOException {
        String jsonString = fixture("fixtures/resource/service/patch/update-name-missing-path.json");

        Optional<Errors> errors = serviceRequestValidatorV2.validateUpdateAttributeRequest(mapper.readTree(jsonString));
        assertThat(errors.isPresent(), is(true));
        assertThat(errors.get().getErrors(), hasSize(1));
        assertThat(errors.get().getErrors().get(0), is("Field [path] is required"));
    }

    @Test
    public void shouldThrowException_whenJsonIsNotAnArray() throws IOException {
        String jsonString = fixture("fixtures/resource/service/patch/update-name-not-json-array.json");

        Optional<Errors> errors = serviceRequestValidatorV2.validateUpdateAttributeRequest(mapper.readTree(jsonString));
        assertThat(errors.isPresent(), is(true));
        assertThat(errors.get().getErrors(), hasSize(1));
        assertThat(errors.get().getErrors().get(0), is("The JSON payload needs to be an array"));
    }

    @Test
    public void shouldAllowNonNumericGatewayAccounts_whenFindingServices() {
        Optional<Errors> errors = serviceRequestValidatorV2.validateFindRequest("non-numeric-id");
        MatcherAssert.assertThat(errors.isPresent(), Is.is(false));
    }

    @Test
    public void shouldFail_whenUpdate_whenServiceNameFieldPresentAndItIsTooLong()  {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "name", "op", "replace", "value", RandomStringUtils.randomAlphanumeric(51));

        Optional<Errors> errors = serviceRequestValidatorV2.validateUpdateAttributeRequest(mapper.valueToTree(Collections.singletonList(payload)));

        assertTrue(errors.isPresent());
        List<String> errorsList = errors.get().getErrors();
        MatcherAssert.assertThat(errorsList.size(), Is.is(1));
        MatcherAssert.assertThat(errorsList, hasItem("Field [value] must have a maximum length of 50 characters"));
    }

    @Test
    public void shouldFail_whenUpdate_whenMissingRequiredField() {
        ImmutableMap<String, String> payload = ImmutableMap.of("value", "example-name");

        Optional<Errors> errors = serviceRequestValidatorV2.validateUpdateAttributeRequest(mapper.valueToTree(Collections.singletonList(payload)));

        assertTrue(errors.isPresent());
        List<String> errorsList = errors.get().getErrors();
        MatcherAssert.assertThat(errorsList.size(), Is.is(2));
        MatcherAssert.assertThat(errorsList, hasItem("Field [path] is required"));
        MatcherAssert.assertThat(errorsList, hasItem("Field [op] is required"));
    }

    @Test
    public void shouldFail_whenUpdate_whenInvalidPath() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "xyz", "op", "replace", "value", "example-name");

        Optional<Errors> errors = serviceRequestValidatorV2.validateUpdateAttributeRequest(mapper.valueToTree(Collections.singletonList(payload)));

        assertTrue(errors.isPresent());
        List<String> errorsList = errors.get().getErrors();
        MatcherAssert.assertThat(errorsList.size(), Is.is(1));
        MatcherAssert.assertThat(errorsList, hasItem("Path [xyz] is invalid"));
    }

    @Test
    public void shouldFail_whenUpdate_whenInvalidOperationForSuppliedPath() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "name", "op", "add", "value", "example-name");

        Optional<Errors> errors = serviceRequestValidatorV2.validateUpdateAttributeRequest(mapper.valueToTree(Collections.singletonList(payload)));

        assertTrue(errors.isPresent());
        List<String> errorsList = errors.get().getErrors();
        MatcherAssert.assertThat(errorsList.size(), Is.is(1));
        MatcherAssert.assertThat(errorsList, hasItem("Operation [add] is invalid for path [name]"));
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

        serviceRequestValidatorV2.validateUpdateMerchantDetailsRequest(payload);
    }

    @Test(expected = ValidationException.class)
    public void shouldFail_updatingMerchantDetails_forEmptyObject() throws ValidationException {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        serviceRequestValidatorV2.validateUpdateMerchantDetailsRequest(payload);
    }

    @Test(expected = ValidationException.class)
    public void shouldFail_updatingMerchantDetails_forMissingMandatoryFields() throws ValidationException {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_LINE1, "line1");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_CITY, "city");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_COUNTRY, "country");
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE, "postcode");

        serviceRequestValidatorV2.validateUpdateMerchantDetailsRequest(payload);
    }

    @Test(expected = ValidationException.class)
    public void shouldFail_updatingMerchantDetails_forBlankStringMandatoryFields() throws ValidationException {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_NAME, "");

        serviceRequestValidatorV2.validateUpdateMerchantDetailsRequest(payload);
    }

    @Test(expected = ValidationException.class)
    public void shouldFail_updatingMerchantDetails_forNullValueMandatoryFields() throws ValidationException {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.set(ServiceRequestValidator.FIELD_MERCHANT_DETAILS_NAME, null);

        serviceRequestValidatorV2.validateUpdateMerchantDetailsRequest(payload);
    }

    @Test
    public void shouldFail_updatingMerchantDetails_whenInvalidEmail() {
        ObjectNode payload = createJsonPayload("invalid@example.com-uk");

        try {
            serviceRequestValidatorV2.validateUpdateMerchantDetailsRequest(payload);
        } catch (ValidationException e) {
            MatcherAssert.assertThat(e.getErrors().getErrors(), hasItem("Field [email] must be a valid email address"));
        }
    }

    @Test
    public void shouldFail_updatingMerchantDetails_whenEmailOver255() {
        String longEmail = RandomStringUtils.randomAlphanumeric(256);
        ObjectNode payload = createJsonPayload(longEmail);

        try {
            serviceRequestValidatorV2.validateUpdateMerchantDetailsRequest(payload);
        } catch (ValidationException e) {
            MatcherAssert.assertThat(e.getErrors().getErrors(), hasItem("Field [email] must have a maximum length of 255 characters"));
        }
    }

    @Test
    public void shouldSuccess_replacingCustomBranding() throws Exception {
        ImmutableMap<String, Object> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", ImmutableMap.of("image_url", "image url", "css_url", "css url"));
        Optional<Errors> errors = serviceRequestValidatorV2.validateUpdateAttributeRequest(mapper.valueToTree(Collections.singletonList(payload)));

        MatcherAssert.assertThat(errors.isPresent(), Is.is(false));
    }

    @Test
    public void shouldSuccess_replacingCustomBranding_forEmptyObject() throws Exception {
        ImmutableMap<String, Object> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", ImmutableMap.of());
        Optional<Errors> errors = serviceRequestValidatorV2.validateUpdateAttributeRequest(mapper.valueToTree(Collections.singletonList(payload)));

        MatcherAssert.assertThat(errors.isPresent(), Is.is(false));
    }

    @Test
    public void shouldError_ifCustomBrandingIsEmptyString() throws Exception {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", "");
        Optional<Errors> errors = serviceRequestValidatorV2.validateUpdateAttributeRequest(mapper.valueToTree(Collections.singletonList(payload)));

        MatcherAssert.assertThat(errors.isPresent(), Is.is(true));
        List<String> errorsList = errors.get().getErrors();
        MatcherAssert.assertThat(errorsList.size(), Is.is(1));
        MatcherAssert.assertThat(errorsList, hasItem("Value for path [custom_branding] must be a JSON"));
    }

    @Test
    public void shouldError_ifCustomBrandingIsNull() throws Exception {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "custom_branding", "op", "replace");
        Optional<Errors> errors = serviceRequestValidatorV2.validateUpdateAttributeRequest(mapper.valueToTree(Collections.singletonList(payload)));

        MatcherAssert.assertThat(errors.isPresent(), Is.is(true));
        List<String> errorsList = errors.get().getErrors();
        MatcherAssert.assertThat(errorsList.size(), Is.is(1));
        MatcherAssert.assertThat(errorsList, hasItem("Value for path [custom_branding] must be a JSON"));
    }

    @Test
    public void shouldError_replacingCustomBranding_ifValueIsNotJSON() throws Exception {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", "&*£&^(P%£");
        Optional<Errors> errors = serviceRequestValidatorV2.validateUpdateAttributeRequest(mapper.valueToTree(Collections.singletonList(payload)));

        MatcherAssert.assertThat(errors.isPresent(), Is.is(true));
        List<String> errorsList = errors.get().getErrors();
        MatcherAssert.assertThat(errorsList.size(), Is.is(1));
        MatcherAssert.assertThat(errorsList, hasItem("Value for path [custom_branding] must be a JSON"));
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
