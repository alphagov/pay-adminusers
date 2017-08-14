package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.List;
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
    public void shouldSuccess_onEmptyJson() throws Exception {
        Optional<Errors> errors = serviceRequestValidator.validateCreateRequest(mapper.readTree("{}"));
        assertFalse(errors.isPresent());

    }

    @Test
    public void shouldSuccess_onNullPayload() throws Exception {
        Optional<Errors> errors = serviceRequestValidator.validateCreateRequest(null);
        assertFalse(errors.isPresent());
    }

    @Test
    public void shouldSuccess_ifNameIsEmpty() throws Exception {
        ImmutableMap<String, String> payload = ImmutableMap.of("name", "");
        Optional<Errors> errors = serviceRequestValidator.validateCreateRequest(mapper.valueToTree(payload));
        assertFalse(errors.isPresent());
    }

    @Test
    public void shouldSuccess_ifGatewayAccountIdsIsEmptyArray() throws Exception {
        ImmutableMap<Object, Object> payload = ImmutableMap.builder()
                .put("gateway_account_ids", new String[]{})
                .build();

        Optional<Errors> errors = serviceRequestValidator.validateCreateRequest(mapper.valueToTree(payload));
        assertFalse(errors.isPresent());
    }

    @Test
    public void shouldSuccess_forValidGatewayAccountIds() throws Exception {

        ImmutableMap<Object, Object> payload = ImmutableMap.builder()
                .put("gateway_account_ids", new String[]{"1", "2"})
                .build();

        Optional<Errors> errors = serviceRequestValidator.validateCreateRequest(mapper.valueToTree(payload));
        assertFalse(errors.isPresent());

    }

    @Test
    public void shouldSuccess_whenUpdate_whenAllFieldPresentAndValid() throws Exception {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "name", "op", "replace", "value", "example-name");

        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertFalse(errors.isPresent());
    }

    @Test
    public void shouldFail_whenUpdate_whenMissingRequiredField() throws Exception {
        ImmutableMap<String, String> payload = ImmutableMap.of("value", "example-name");

        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertTrue(errors.isPresent());
        List<String> errorsList = errors.get().getErrors();
        assertThat(errorsList.size(), is(2));
        assertThat(errorsList, hasItem("Field [path] is required"));
        assertThat(errorsList, hasItem("Field [op] is required"));
    }

    @Test
    public void shouldFail_whenUpdate_whenInvalidPath() throws Exception {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "xyz", "op", "replace", "value", "example-name");

        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertTrue(errors.isPresent());
        List<String> errorsList = errors.get().getErrors();
        assertThat(errorsList.size(), is(1));
        assertThat(errorsList, hasItem("Path [xyz] is invalid"));
    }

    @Test
    public void shouldFail_whenUpdate_whenInvalidOperationForSuppliedPath() throws Exception {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "name", "op", "add", "value", "example-name");

        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertTrue(errors.isPresent());
        List<String> errorsList = errors.get().getErrors();
        assertThat(errorsList.size(), is(1));
        assertThat(errorsList, hasItem("Operation [add] is invalid for path [name]"));
    }

    @Test
    public void shouldSuccess_replacingCustomBranding() throws Exception {
        ImmutableMap<String, Object> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", ImmutableMap.of("image_url","image url", "css_url","css url"));
        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertThat(errors.isPresent(), is(false));
    }

    @Test
    public void shouldSuccess_replacingCustomBranding_forEmptyObject() throws Exception {
        ImmutableMap<String, Object> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", ImmutableMap.of());
        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertThat(errors.isPresent(), is(false));
    }

    @Test
    public void shouldError_ifCustomBrandingIsEmptyString() throws Exception {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", "");
        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertThat(errors.isPresent(), is(true));
        List<String> errorsList = errors.get().getErrors();
        assertThat(errorsList.size(), is(1));
        assertThat(errorsList, hasItem("Value for path [custom_branding] must be a JSON"));
    }

    @Test
    public void shouldError_ifCustomBrandingIsNull() throws Exception {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "custom_branding", "op", "replace");
        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertThat(errors.isPresent(), is(true));
        List<String> errorsList = errors.get().getErrors();
        assertThat(errorsList.size(), is(1));
        assertThat(errorsList, hasItem("Value for path [custom_branding] must be a JSON"));
    }

    @Test
    public void shouldError_replacingCustomBranding_ifValueIsNotJSON() throws Exception {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", "&*£&^(P%£");
        Optional<Errors> errors = serviceRequestValidator.validateUpdateAttributeRequest(mapper.valueToTree(payload));

        assertThat(errors.isPresent(), is(true));
        List<String> errorsList = errors.get().getErrors();
        assertThat(errorsList.size(), is(1));
        assertThat(errorsList, hasItem("Value for path [custom_branding] must be a JSON"));
    }
}
