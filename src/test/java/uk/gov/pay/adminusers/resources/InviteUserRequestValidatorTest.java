package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
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
    void validateOtpValidationRequest_shouldError_ifAllMandatoryFieldsAreMissing() throws Exception {

        String invalidPayload = "{}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);
        Optional<Errors> optionalErrors = validator.validateOtpValidationRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(2));
        assertThat(errors.getErrors(), hasItems(
                "Field [code] is required",
                "Field [otp] is required"));
    }

    @Test
    void validateOtpValidationRequest_shouldError_ifCodeFieldIsMissing() throws Exception {

        String invalidPayload = "{" +
                "\"otp\": \"an-otp-code\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);
        Optional<Errors> optionalErrors = validator.validateOtpValidationRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [code] is required"));
    }

    @Test
    void validateOtpValidationRequest_shouldError_ifOtpFieldIsMissing() throws Exception {

        String invalidPayload = "{" +
                "\"code\": \"a-code\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);
        Optional<Errors> optionalErrors = validator.validateOtpValidationRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [otp] is required"));
    }
}
