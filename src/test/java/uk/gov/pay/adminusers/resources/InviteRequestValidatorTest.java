package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;

public class InviteRequestValidatorTest {

    private InviteRequestValidator validator;

    private ObjectMapper objectMapper;

    @Before
    public void before() throws Exception {
        validator = new InviteRequestValidator(new RequestValidations());
        objectMapper = new ObjectMapper();
    }

    @Test
    public void validateCreateRequest_shouldError_ifAllMandatoryFieldsAreMissing() throws Exception {

        String invalidPayload = "{}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);
        Optional<Errors> optionalErrors = validator.validateCreateRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(3));
        assertThat(errors.getErrors(), hasItems(
                "Field [sender] is required",
                "Field [email] is required",
                "Field [role_name] is required"));
    }

    @Test
    public void validateCreateRequest_shouldError_ifRoleNameFieldIsMissing() throws Exception {

        String invalidPayload = "{" +
                "\"sender\": \"12345abc\"," +
                "\"email\": \"email@example.com\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);

        Optional<Errors> optionalErrors = validator.validateCreateRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [role_name] is required"));
    }

    @Test
    public void validateCreateRequest_shouldError_ifEmailFieldIsMissing() throws Exception {

        String invalidPayload = "{" +
                "\"sender\": \"12345abc\"," +
                "\"role_name\": \"admin\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);

        Optional<Errors> optionalErrors = validator.validateCreateRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [email] is required"));
    }

    @Test
    public void validateCreateRequest_shouldError_ifSenderFieldIsMissing() throws Exception {

        String invalidPayload = "{" +
                "\"email\": \"email@example.com\"," +
                "\"role_name\": \"admin\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);

        Optional<Errors> optionalErrors = validator.validateCreateRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [sender] is required"));
    }

    @Test
    public void validateGenerateOtpRequest_shouldError_ifAllMandatoryFieldsAreMissing() throws Exception {

        String invalidPayload = "{}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);
        Optional<Errors> optionalErrors = validator.validateGenerateOtpRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(3));
        assertThat(errors.getErrors(), hasItems(
                "Field [code] is required",
                "Field [telephone_number] is required",
                "Field [password] is required"));
    }

    @Test
    public void validateGenerateOtpRequest_shouldError_ifCodeFieldIsMissing() throws Exception {

        String invalidPayload = "{" +
                "\"telephone_number\": \"a-telephone_number\"," +
                "\"password\": \"a-password\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);

        Optional<Errors> optionalErrors = validator.validateGenerateOtpRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [code] is required"));
    }

    @Test
    public void validateGenerateOtpRequest_shouldError_ifTelephoneNumberFieldIsMissing() throws Exception {

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
    public void validateGenerateOtpRequest_shouldError_ifPasswordFieldIsMissing() throws Exception {

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
    public void validateResendOtpRequest_shouldError_ifAllMandatoryFieldsAreMissing() throws Exception {

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
    public void validateResendOtpRequest_shouldError_ifCodeFieldIsMissing() throws Exception {

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
    public void validateResendOtpRequest_shouldError_ifTelephoneNumberFieldIsMissing() throws Exception {

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
    public void validateOtpValidationRequest_shouldError_ifAllMandatoryFieldsAreMissing() throws Exception {

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
    public void validateOtpValidationRequest_shouldError_ifCodeFieldIsMissing() throws Exception {

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
    public void validateOtpValidationRequest_shouldError_ifOtpFieldIsMissing() throws Exception {

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
