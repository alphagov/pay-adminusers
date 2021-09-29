package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import javax.ws.rs.WebApplicationException;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void validateCreateUserRequest_shouldError_ifAllMandatoryFieldsAreMissing() throws Exception {

        String invalidPayload = "{}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);
        Optional<Errors> optionalErrors = validator.validateCreateUserRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(4));
        assertThat(errors.getErrors(), hasItems(
                "Field [service_external_id] is required",
                "Field [sender] is required",
                "Field [email] is required",
                "Field [role_name] is required"));
    }

    @Test
    void validateCreateUserRequest_shouldError_ifServiceExternalIdFieldIsMissing() throws Exception {

        String invalidPayload = "{" +
                "\"sender\": \"12345abc\"," +
                "\"email\": \"email@example.com\"," +
                "\"role_name\": \"admin\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);

        Optional<Errors> optionalErrors = validator.validateCreateUserRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [service_external_id] is required"));
    }

    @Test
    void validateCreateUserRequest_shouldError_ifRoleNameFieldIsMissing() throws Exception {

        String invalidPayload = "{" +
                "\"service_external_id\": \"service123\"," +
                "\"sender\": \"12345abc\"," +
                "\"email\": \"email@example.com\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);

        Optional<Errors> optionalErrors = validator.validateCreateUserRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [role_name] is required"));
    }

    @Test
    void validateCreateUserRequest_shouldError_ifEmailFieldIsMissing() throws Exception {

        String invalidPayload = "{" +
                "\"service_external_id\": \"service123\"," +
                "\"sender\": \"12345abc\"," +
                "\"role_name\": \"admin\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);

        Optional<Errors> optionalErrors = validator.validateCreateUserRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [email] is required"));
    }

    @Test
    void validateCreateUserRequest_shouldError_ifSenderFieldIsMissing() throws Exception {

        String invalidPayload = "{" +
                "\"service_external_id\": \"service123\"," +
                "\"email\": \"email@example.com\"," +
                "\"role_name\": \"admin\"" +
                "}";
        JsonNode jsonNode = objectMapper.readTree(invalidPayload);

        Optional<Errors> optionalErrors = validator.validateCreateUserRequest(jsonNode);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(1));
        assertThat(errors.getErrors(), hasItems(
                "Field [sender] is required"));
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

    @Test
    void validateCreateServiceRequest_shouldSuccess_ifAllFieldsArePresentAndValidEmailDomain() {
        Map<String, String> payload = Map.of("email", "example@example.gov.uk", "telephone_number", "01134960000", "password", "super-secure-password");
        JsonNode payloadNode = objectMapper.valueToTree(payload);
        Optional<Errors> errors = validator.validateCreateServiceRequest(payloadNode);

        assertFalse(errors.isPresent());
    }

    @Test
    void validateCreateServiceRequest_shouldSuccess_ifOnlyEmailFieldIsPresentAndValidEmailDomain() {
        Map<String, String> payload = Map.of("email", "example@example.gov.uk");
        JsonNode payloadNode = objectMapper.valueToTree(payload);
        Optional<Errors> errors = validator.validateCreateServiceRequest(payloadNode);

        assertFalse(errors.isPresent());
    }

    @Test
    void validateCreateServiceRequest_shouldFail_ifMissingRequiredField() {
        Map<String, String> payload = Map.of( "telephone_number", "01134960000", "password", "super-secure-password");
        JsonNode payloadNode = objectMapper.valueToTree(payload);
        Optional<Errors> errors = validator.validateCreateServiceRequest(payloadNode);

        assertTrue(errors.isPresent());
        assertThat(errors.get().getErrors(),hasItems("Field [email] is required"));
        assertThat(errors.get().getErrors().size(),is(1));
    }

    @Test
    void validateCreateServiceRequest_shouldFail_ifInvalidEmailFormat() {
        Map<String, String> payload = Map.of( "email", "exampleatexample.com", "telephone_number", "01134960000", "password", "super-secure-password");
        JsonNode payloadNode = objectMapper.valueToTree(payload);
        Optional<Errors> errors = validator.validateCreateServiceRequest(payloadNode);

        assertTrue(errors.isPresent());
        assertThat(errors.get().getErrors(),hasItems("Field [email] must be a valid email address"));
        assertThat(errors.get().getErrors().size(),is(1));
    }

    @Test
    void validateCreateServiceRequest_shouldFail_ifEmailAddressNotPublicSector() {
        Map<String, String> payload = Map.of( "email", "example@example.com","telephone_number", "01134960000", "password", "super-secure-password");
        JsonNode payloadNode = objectMapper.valueToTree(payload);
        assertThrows(WebApplicationException.class, () -> validator.validateCreateServiceRequest(payloadNode));
    }

    @Test
    void validateCreateServiceRequest_shouldFail_ifTelephoneNumberIsInvalid() {
        Map<String, String> payload = Map.of( "email", "example@example.gov.uk","telephone_number", "0770090000A", "password", "super-secure-password");
        JsonNode payloadNode = objectMapper.valueToTree(payload);
        Optional<Errors> errors = validator.validateCreateServiceRequest(payloadNode);

        assertTrue(errors.isPresent());
        assertThat(errors.get().getErrors(),hasItems("Field [telephone_number] must be a valid telephone number"));
        assertThat(errors.get().getErrors().size(),is(1));
    }
}
