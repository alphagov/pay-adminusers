package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;
import uk.gov.service.payments.commons.api.exception.ValidationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InviteUserRequestValidatorTest {

    private InviteRequestValidator validator;

    private static ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void before() {
        validator = new InviteRequestValidator(new RequestValidations());
    }

    @Test
    void validateGenerateOtpRequest_shouldError_ifAllMandatoryFieldsAreMissing() throws Exception {

        String invalidPayload = "{}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);
        Optional<Errors> optionalErrors = validator.validateGenerateOtpRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(2));
        assertThat(errors.getErrors(), hasItems(
                "Field [telephone_number] is required",
                "Field [password] is required"));
    }

    @Test
    void validateGenerateOtpRequest_shouldError_ifCodeFieldIsMissing() throws Exception {

        String invalidPayload = "{" +
                "\"password\": \"a-password\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);

        Optional<Errors> optionalErrors = validator.validateGenerateOtpRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [telephone_number] is required"));
    }

    @Test
    void validateGenerateOtpRequest_shouldError_ifTelephoneNumberFieldIsMissing() throws Exception {

        String invalidPayload = "{" +
                "\"code\": \"a-code\"," +
                "\"password\": \"a-password\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);

        Optional<Errors> optionalErrors = validator.validateGenerateOtpRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [telephone_number] is required"));
    }

    @Test
    void validateGenerateOtpRequest_shouldError_ifPasswordFieldIsMissing() throws Exception {

        String invalidPayload = "{" +
                "\"code\": \"a-code\"," +
                "\"telephone_number\": \"a-telephone_number\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);

        Optional<Errors> optionalErrors = validator.validateGenerateOtpRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [password] is required"));
    }

    @Test
    void validateResendOtpRequest_shouldError_ifAllMandatoryFieldsAreMissing() throws Exception {

        String invalidPayload = "{}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);
        Optional<Errors> optionalErrors = validator.validateResendOtpRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(2));
        assertThat(errors.getErrors(), hasItems(
                "Field [code] is required",
                "Field [telephone_number] is required"));
    }

    @Test
    void validateResendOtpRequest_shouldError_ifCodeFieldIsMissing() throws Exception {

        String invalidPayload = "{" +
                "\"telephone_number\": \"a-telephone_number\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);

        Optional<Errors> optionalErrors = validator.validateResendOtpRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [code] is required"));
    }

    @Test
    void validateResendOtpRequest_shouldError_ifTelephoneNumberFieldIsMissing() throws Exception {

        String invalidPayload = "{" +
                "\"code\": \"a-code\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);

        Optional<Errors> optionalErrors = validator.validateResendOtpRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [telephone_number] is required"));
    }

    @Test
    void validateUpdateRequest_shouldNotThrowForValidRequest() {
        JsonNode request = objectMapper.valueToTree(
                List.of(
                        Map.of("path", "telephone_number",
                                "op", "replace",
                                "value", "+441134960000"),
                        Map.of("path", "password",
                                "op", "replace",
                                "value", "something-allowed")
                ));
        
        assertDoesNotThrow(() -> validator.validatePatchRequest(request));
    }

    @Test
    void validateUpdateRequest_shouldThrowWhenTelephoneNumberIsNotAString() {
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "telephone_number",
                        "op", "replace",
                        "value", 123)));
        var thrown = assertThrows(ValidationException.class, () -> validator.validatePatchRequest(request));
        assertThat(thrown.getErrors(), hasItem("Value for path [telephone_number] must be a string"));
    }

    @Test
    void validateUpdateRequest_shouldThrowWhenTelephoneNumberIsNotAValidPhoneNumber() {
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "telephone_number",
                        "op", "replace",
                        "value", "0000")));
        var thrown = assertThrows(ValidationException.class, () -> validator.validatePatchRequest(request));
        assertThat(thrown.getErrors(), hasItem("Value for path [telephone_number] must be a valid telephone number"));
    }

    @Test
    void validateUpdateRequest_shouldThrowWhenPasswordIsNotAString() {
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "password",
                        "op", "replace",
                        "value", 123)));
        var thrown = assertThrows(ValidationException.class, () -> validator.validatePatchRequest(request));
        assertThat(thrown.getErrors(), hasItem("Value for path [password] must be a string"));
    }


}
