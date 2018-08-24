package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ServiceUpdateOperationValidatorTest {

    private ObjectMapper mapper = new ObjectMapper();
    private ServiceUpdateOperationValidator serviceUpdateOperationValidator = new ServiceUpdateOperationValidator(new RequestValidations());

    @Test
    public void shouldSuccess_whenUpdate_withAllFieldsPresentAndValid() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "name", "op", "replace", "value", "example-name");

        List<String> errors = serviceUpdateOperationValidator.validate(mapper.valueToTree(payload));
        
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void shouldFail_whenUpdate_whenServiceNameFieldPresentAndItIsTooLong() {
        ImmutableMap<String, String> payload = ImmutableMap.of("path", "name", "op", "replace", "value", RandomStringUtils.randomAlphanumeric(51));

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
        ImmutableMap<String, Object> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", ImmutableMap.of("image_url", "image url", "css_url", "css url"));

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

}
