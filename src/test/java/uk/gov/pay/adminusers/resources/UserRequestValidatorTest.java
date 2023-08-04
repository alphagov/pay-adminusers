package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserRequestValidatorTest {

    private UserRequestValidator validator;

    private static ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void before() {
        validator = new UserRequestValidator(new RequestValidations());
    }

    @Test
    void shouldError_ifAllMandatoryFieldsAreMissing() throws Exception {
        String invalidPayload = "{}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);
        Optional<Errors> optionalErrors = validator.validateCreateRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(3));
        assertThat(errors.getErrors(), hasItems(
                "Field [email] is required",
                "Field [telephone_number] is required",
                "Field [role_name] is required"));
    }

    @Test
    void shouldError_ifSomeMandatoryFieldsAreMissing() throws Exception {
        String invalidPayload = "{" +
                "\"email\": \"email@example.com\"," +
                "\"otp_key\": \"12345\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);

        Optional<Errors> optionalErrors = validator.validateCreateRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(2));
        assertThat(errors.getErrors(), hasItems(
                "Field [role_name] is required",
                "Field [telephone_number] is required"));

    }

    @Test
    void shouldError_ifMandatoryPatchFieldsAreMissing() {
        JsonNode invalidPayload = mock(JsonNode.class);
        mockValidValuesFor(invalidPayload, Map.of("foo", "blah", "bar", "blah@blah.com"));
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
    void shouldError_ifPathNotAllowed_whenPatching() {
        JsonNode invalidPayload = mock(JsonNode.class);
        mockValidValuesFor(invalidPayload, Map.of("op", "append", "path", "version", "value", "1"));
        Optional<Errors> optionalErrors = validator.validatePatchRequest(invalidPayload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems("Patching path [version] not allowed"));
    }

    @Test
    void shouldError_ifPathOperationNotValid_whenPatching() {
        JsonNode invalidPayload = mock(JsonNode.class);
        mockValidValuesFor(invalidPayload, Map.of("op", "replace", "path", "sessionVersion", "value", "1"));
        Optional<Errors> optionalErrors = validator.validatePatchRequest(invalidPayload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems("Operation [replace] not allowed for path [sessionVersion]"));
    }

    @Test
    void shouldError_ifSessionVersionNotNumeric_whenPatching() {
        JsonNode payload = objectMapper.valueToTree(Map.of("op", "append", "path", "sessionVersion", "value", "1r"));
        Optional<Errors> optionalErrors = validator.validatePatchRequest(payload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems("path [sessionVersion] must contain a value of positive integer"));
    }

    @Test
    void shouldError_ifDisabledNotBoolean_whenPatching() {
        JsonNode payload = objectMapper.valueToTree(Map.of("op", "replace", "path", "disabled", "value", "1r"));
        Optional<Errors> optionalErrors = validator.validatePatchRequest(payload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems("path [disabled] must be contain value [true | false]"));
    }

    @Test
    void shouldSuccess_forDisabled_whenPatching() {
        JsonNode payload = objectMapper.valueToTree(Map.of("op", "replace", "path", "disabled", "value", "true"));
        Optional<Errors> optionalErrors = validator.validatePatchRequest(payload);

        assertFalse(optionalErrors.isPresent());
    }

    @Test
    void shouldSuccess_forSessionVersion_whenPatching() {
        JsonNode payload = objectMapper.valueToTree(Map.of("op", "append", "path", "sessionVersion", "value", "2"));
        Optional<Errors> optionalErrors = validator.validatePatchRequest(payload);

        assertFalse(optionalErrors.isPresent());
    }

    @Test
    void shouldSuccess_replacingTelephoneNumber_whenPatchingLocalTelephoneNumber() {
        JsonNode payload = objectMapper.valueToTree(Map.of("op", "replace", "path", "telephone_number", "value", "01134960000"));
        Optional<Errors> optionalErrors = validator.validatePatchRequest(payload);

        assertFalse(optionalErrors.isPresent());
    }

    @Test
    void shouldSuccess_replacingTelephoneNumber_whenPatchingInternationalTelephoneNumber() {
        JsonNode payload = objectMapper.valueToTree(Map.of("op", "replace", "path", "telephone_number", "value", "+441134960000"));
        Optional<Errors> optionalErrors = validator.validatePatchRequest(payload);

        assertFalse(optionalErrors.isPresent());
    }

    @Test
    void shouldError_replacingTelephoneNumber_whenPatchingInvalidTelephoneNumber() {
        JsonNode payload = objectMapper.valueToTree(Map.of("op", "replace", "path", "telephone_number", "value", "(╯°□°）╯︵ ┻━┻"));
        Optional<Errors> optionalErrors = validator.validatePatchRequest(payload);

        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems("path [telephone_number] must contain a valid telephone number"));
    }
    
    @Test
    void shouldError_whenPatchingInvalidEmail() {
        JsonNode payload = objectMapper.valueToTree(Map.of("op", "replace", "path", "email", "value", "(╯°□°）╯︵ ┻━┻"));
        Optional<Errors> optionalErrors = validator.validatePatchRequest(payload);

        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems("path [email] must contain a valid email"));
    }

    @Test
    void shouldSuccess_whenAddingServiceRole() {
        JsonNode payload = objectMapper.valueToTree(Map.of("service_external_id", "blah-blah", "role_name", "blah"));
        Optional<Errors> optionalErrors = validator.validateAssignServiceRequest(payload);

        assertFalse(optionalErrors.isPresent());
    }

    @Test
    void shouldError_whenAddingServiceRole_ifRequiredParamMissing() {
        JsonNode payload = objectMapper.valueToTree(Map.of("service_external_id", "blah-blah"));
        Optional<Errors> optionalErrors = validator.validateAssignServiceRequest(payload);

        Errors errors = optionalErrors.get();
        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [role_name] is required"));
    }

    @Test
    void shouldError_ifTelephoneNumberFieldIsInvalid() throws Exception {
        String invalidPayload = "{" +
                "\"password\": \"a-password\"," +
                "\"email\": \"email@example.com\"," +
                "\"gateway_account_ids\": [\"1\"]," +
                "\"telephone_number\": \"(╯°□°）╯︵ ┻━┻\"," +
                "\"otp_key\": \"12345\"," +
                "\"role_name\": \"a-role\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);
        Optional<Errors> optionalErrors = validator.validateCreateRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [telephone_number] must be a valid telephone number"));

    }

    @Test
    void shouldError_ifFieldsAreBiggerThanMaxLength() throws Exception {
        String invalidPayload = "{" +
                "\"password\": \"" + RandomStringUtils.randomAlphanumeric(256) + "\"," +
                "\"email\": \"" + RandomStringUtils.randomAlphanumeric(255) + "\"," +
                "\"gateway_account_ids\": [\"1\"]," +
                "\"telephone_number\": \"07990000000\"," +
                "\"otp_key\": \"12345\"," +
                "\"role_name\": \"a-role\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);
        Optional<Errors> optionalErrors = validator.validateCreateRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [email] must have a maximum length of 254 characters"));

    }

    @Test
    void shouldReturnEmpty_ifAllValidationsArePassed() throws Exception {
        String validPayload = "{" +
                "\"password\": \"a-password\"," +
                "\"email\": \"email@example.com\"," +
                "\"gateway_account_ids\": [\"1\"]," +
                "\"telephone_number\": \"01134960000\"," +
                "\"otp_key\": \"12345\"," +
                "\"role_name\": \"a-role\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(validPayload);
        Optional<Errors> optionalErrors = validator.validateCreateRequest(jsonNode);

        assertFalse(optionalErrors.isPresent());
    }

    @Test
    void shouldSuccess_ifValidSearchRequest_whenFindingAUser() {
        JsonNode payload = objectMapper.valueToTree(Map.of("email", "some-existing-user"));
        Optional<Errors> optionalErrors = validator.validateFindRequest(payload);

        assertThat(optionalErrors.isPresent(), is(false));
    }

    @Test
    void shouldSuccess_ifNoBody_whenValidateNewSecondFactorPasscodeRequest() {
        Optional<Errors> optionalErrors = validator.validateNewSecondFactorPasscodeRequest(null);

        assertThat(optionalErrors.isPresent(), is(false));
    }

    @Test
    void shouldSuccess_ifProvisionalNotPresent_whenValidateNewSecondFactorPasscodeRequest() {
        JsonNode payload = objectMapper.valueToTree(Collections.emptyMap());

        Optional<Errors> optionalErrors = validator.validateNewSecondFactorPasscodeRequest(payload);

        assertThat(optionalErrors.isPresent(), is(false));
    }

    @Test
    void shouldSuccess_ifProvisionalTrue_whenValidateNewSecondFactorPasscodeRequest() {
        JsonNode payload = objectMapper.valueToTree(Map.of("provisional", true));

        Optional<Errors> optionalErrors = validator.validateNewSecondFactorPasscodeRequest(payload);

        assertThat(optionalErrors.isPresent(), is(false));
    }

    @Test
    void shouldSuccess_ifProvisionalFalse_whenValidateNewSecondFactorPasscodeRequest() {
        JsonNode payload = objectMapper.valueToTree(Map.of("provisional", false));

        Optional<Errors> optionalErrors = validator.validateNewSecondFactorPasscodeRequest(payload);

        assertThat(optionalErrors.isPresent(), is(false));
    }

    @Test
    void shouldError_ifProvisionalNotBoolean_whenValidateNewSecondFactorPasscodeRequest() {
        JsonNode payload = objectMapper.valueToTree(Map.of("provisional", "maybe"));

        Optional<Errors> optionalErrors = validator.validateNewSecondFactorPasscodeRequest(payload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems("Field [provisional] must be a boolean"));
    }

    @Test
    void shouldError_ifCodeMissing_whenValidate2faActivateRequest() {
        JsonNode payload = objectMapper.valueToTree(Map.of("second_factor", "SMS"));

        Optional<Errors> optionalErrors = validator.validate2faActivateRequest(payload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems("Field [code] is required"));
    }

    @Test
    void shouldError_ifCodeNotNumeric_whenValidate2faActivateRequest() {
        JsonNode payload = objectMapper.valueToTree(Map.of("code", "I am not a number, I’m a free man!",
                "second_factor", "SMS"));

        Optional<Errors> optionalErrors = validator.validate2faActivateRequest(payload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems("Field [code] must be a number"));
    }

    @Test
    void shouldError_ifSecondFactorMissing_whenValidate2faActivateRequest() {
        JsonNode payload = objectMapper.valueToTree(Map.of("code", 123456));

        Optional<Errors> optionalErrors = validator.validate2faActivateRequest(payload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems("Field [second_factor] is required"));
    }

    @Test
    void shouldError_ifSecondFactorInvalid_whenValidate2faActivateRequest() {
        JsonNode payload = objectMapper.valueToTree(Map.of("code", 123456,
                "second_factor", "PINKY_SWEAR"));

        Optional<Errors> optionalErrors = validator.validate2faActivateRequest(payload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems("Invalid second_factor [PINKY_SWEAR]"));
    }

    @Test
    void shouldError_ifRequiredFieldsMissing_whenFindingAUser() throws Exception {
        JsonNode payload = objectMapper.readTree("{}");
        Optional<Errors> optionalErrors = validator.validateFindRequest(payload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems("Field [email] is required"));
    }

    private void mockValidValuesFor(JsonNode mockJsonNode, Map<String, String> mockFieldValues) {
        for (Map.Entry<String, String> mockFieldValue : mockFieldValues.entrySet()) {
            JsonNode fieldMock = mock(JsonNode.class);
            when(fieldMock.asText()).thenReturn(mockFieldValue.getValue());
            when(mockJsonNode.get(mockFieldValue.getKey())).thenReturn(fieldMock);
        }
        when(mockJsonNode.fieldNames()).thenReturn(mockFieldValues.keySet().iterator());
    }
}
