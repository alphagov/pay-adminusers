package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.tuple.Pair.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserRequestValidatorTest {

    private UserRequestValidator validator;

    @Before
    public void before() throws Exception {
        validator = new UserRequestValidator(new RequestValidations());
    }

    @Test
    public void shouldError_ifAllMandatoryFieldsAreMissing() throws Exception {
        JsonNode invalidPayload = mock(JsonNode.class);
        when(invalidPayload.get(anyString())).thenReturn(null);
        Optional<Errors> optionalErrors = validator.validateCreateRequest(invalidPayload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(4));
        assertThat(errors.getErrors(), hasItems(
                "Field [username] is required",
                "Field [email] is required",
                "Field [telephone_number] is required",
                "Field [role_name] is required"));

    }

    @Test
    public void shouldError_ifSomeMandatoryFieldsAreMissing() throws Exception {
        JsonNode invalidPayload = mock(JsonNode.class);
        mockValidValuesFor(invalidPayload,
                of("username", "blah"), of("email", "blah@blah.com"), of("otp_key", "blahblah"));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(invalidPayload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(2));
        assertThat(errors.getErrors(), hasItems(
                "Field [role_name] is required",
                "Field [telephone_number] is required"));

    }

    @Test
    public void shouldError_ifGatewayAccountFieldsAreMissing() throws Exception {
        JsonNode invalidPayload = mock(JsonNode.class);
        mockValidValuesFor(invalidPayload,
                of("username", "blah"), of("password", "blah pass"), of("email", "blah@blah.com"),
                of("telephone_number", "telephoneNumber"), of("otp_key", "blahblah"),
                of("role_name","boo"));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(invalidPayload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Must have one of [gateway_account_id,gateway_account_ids]"));
    }

    @Test
    public void shouldError_ifMandatoryPatchFieldsAreMissing() throws Exception {
        JsonNode invalidPayload = mock(JsonNode.class);
        mockValidValuesFor(invalidPayload, of("foo", "blah"), of("bar", "blah@blah.com"));
        Optional<Errors> optionalErrors = validator.validatePatchRequest(invalidPayload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(3));
        assertThat(errors.getErrors(), hasItems(
                "Field [op] is required",
                "Field [path] is required",
                "Field [value] is required"));
    }

    @Test
    public void shouldError_ifPathNotAllowed_whenPatching() throws Exception {
        JsonNode invalidPayload = mock(JsonNode.class);
        mockValidValuesFor(invalidPayload, of("op", "append"), of("path", "version"), of("value", "1"));
        Optional<Errors> optionalErrors = validator.validatePatchRequest(invalidPayload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems("Patching path [version] not allowed"));
    }

    @Test
    public void shouldError_ifPathOperationNotValid_whenPatching() throws Exception {
        JsonNode invalidPayload = mock(JsonNode.class);
        mockValidValuesFor(invalidPayload, of("op", "replace"), of("path", "sessionVersion"), of("value", "1"));
        Optional<Errors> optionalErrors = validator.validatePatchRequest(invalidPayload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems("Operation [replace] not allowed for path [sessionVersion]"));
    }

    @Test
    public void shouldError_ifSessionVersionNotNumeric_whenPatching() throws Exception {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("op", "append", "path", "sessionVersion", "value", "1r"));
        Optional<Errors> optionalErrors = validator.validatePatchRequest(payload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems("path [sessionVersion] must contain a value of positive integer"));
    }

    @Test
    public void shouldError_ifDisabledNotBoolean_whenPatching() throws Exception {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("op", "replace", "path", "disabled", "value", "1r"));
        Optional<Errors> optionalErrors = validator.validatePatchRequest(payload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems("path [disabled] must be contain value [true | false]"));
    }

    @Test
    public void shouldSuccess_forDisabled_whenPatching() throws Exception {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("op", "replace", "path", "disabled", "value", "true"));
        Optional<Errors> optionalErrors = validator.validatePatchRequest(payload);

        assertFalse(optionalErrors.isPresent());
    }

    @Test
    public void shouldSuccess_forSessionVersion_whenPatching() throws Exception {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("op", "append", "path", "sessionVersion", "value", "2"));
        Optional<Errors> optionalErrors = validator.validatePatchRequest(payload);

        assertFalse(optionalErrors.isPresent());
    }

    @Test
    public void shouldError_ifNumericFieldsAreNotNumeric() throws Exception {
        JsonNode invalidPayload = mock(JsonNode.class);
        mockValidValuesFor(invalidPayload,
                of("username", "blah"), of("password", "blah pass"), of("gateway_account_id", "gatewayAccountId"),
                of("telephone_number", "telephoneNumber"), of("email", "blah@blah.com"), of("otp_key", "blahblah"),
                of("role_name","boo"));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(invalidPayload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [telephone_number] must be a number"));

    }

    @Test
    public void shouldError_ifFieldsAreBiggerThanMaxLength() throws Exception {
        JsonNode invalidPayload = mock(JsonNode.class);
        mockValidValuesFor(invalidPayload,
                of("username", RandomStringUtils.randomAlphanumeric(256)), of("password", RandomStringUtils.randomAlphanumeric(256)), of("gateway_account_id", "123"),
                of("telephone_number", "07990000000"), of("email", "blah@blah.com"), of("otp_key", "blahblah"),
                of("role_name","boo"));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(invalidPayload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [username] must have a maximum length of 255 characters"));

    }

    @Test
    public void shouldReturnEmpty_ifAllValidationsArePassed() throws Exception {
        JsonNode invalidPayload = mock(JsonNode.class);
        mockValidValuesFor(invalidPayload,
                of("username", "blah"), of("gateway_account_id", "1"),
                of("telephone_number", "3534876538"), of("email", "blah@blah.com"),
                of("role_name","yah"));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(invalidPayload);

        assertFalse(optionalErrors.isPresent());
    }

    private void mockValidValuesFor(JsonNode mockJsonNode, Pair<String, String>... mockFieldValues) {
        for (Pair<String, String> mockFieldValue : mockFieldValues) {
            JsonNode fieldMock = mock(JsonNode.class);
            when(fieldMock.asText()).thenReturn(mockFieldValue.getRight());
            when(mockJsonNode.get(mockFieldValue.getLeft())).thenReturn(fieldMock);
        };
        when(mockJsonNode.fieldNames()).thenReturn(Arrays.stream(mockFieldValues).map(Pair::getKey).iterator());
    }
}
