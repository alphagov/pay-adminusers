package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import uk.gov.pay.adminusers.model.InviteServiceRequest;
import uk.gov.pay.adminusers.model.InviteValidateOtpRequest;
import uk.gov.pay.adminusers.service.AdminUsersExceptions;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.utils.telephonenumber.TelephoneNumberUtility;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_CODE;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_PASSWORD;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_TELEPHONE_NUMBER;
import static uk.gov.pay.adminusers.model.InviteUserRequest.FIELD_EMAIL;
import static uk.gov.pay.adminusers.model.InviteUserRequest.FIELD_ROLE_NAME;
import static uk.gov.pay.adminusers.model.InviteUserRequest.FIELD_SENDER;
import static uk.gov.pay.adminusers.model.InviteUserRequest.FIELD_SERVICE_EXTERNAL_ID;
import static uk.gov.pay.adminusers.model.InviteValidateOtpRequest.FIELD_OTP;
import static uk.gov.pay.adminusers.utils.email.EmailValidator.isPublicSectorEmail;
import static uk.gov.pay.adminusers.utils.email.EmailValidator.isValid;

public class InviteRequestValidator {

    private static final int MAX_LENGTH_CODE = 255;
    private final RequestValidations requestValidations;


    @Inject
    public InviteRequestValidator(RequestValidations requestValidations) {
        this.requestValidations = requestValidations;
    }

    public Optional<Errors> validateCreateUserRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkExistsAndNotEmpty(payload, FIELD_SERVICE_EXTERNAL_ID, FIELD_EMAIL, FIELD_ROLE_NAME, FIELD_SENDER);
        return missingMandatoryFields.map(Errors::from);
    }

    public Optional<Errors> validateGenerateOtpRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkExistsAndNotEmpty(payload, FIELD_TELEPHONE_NUMBER, FIELD_PASSWORD);
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }
        if (payload.get(FIELD_CODE) != null) {
            Optional<List<String>> invalidLength = requestValidations.checkMaxLength(payload, MAX_LENGTH_CODE, FIELD_CODE);
            return invalidLength.map(Errors::from);
        }
        return Optional.empty();
    }


    public Optional<Errors> validateResendOtpRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkExistsAndNotEmpty(payload, FIELD_CODE, FIELD_TELEPHONE_NUMBER);
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }
        Optional<List<String>> invalidLength = requestValidations.checkMaxLength(payload, MAX_LENGTH_CODE, FIELD_CODE);
        return invalidLength.map(Errors::from);
    }

    public Optional<Errors> validateOtpValidationRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkExistsAndNotEmpty(payload, InviteValidateOtpRequest.FIELD_CODE, FIELD_OTP);
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }
        Optional<List<String>> invalidLength = requestValidations.checkMaxLength(payload, MAX_LENGTH_CODE, InviteValidateOtpRequest.FIELD_CODE);
        return invalidLength.map(Errors::from);
    }

    public Optional<Errors> validateCreateServiceRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkExistsAndNotEmpty(payload, InviteServiceRequest.FIELD_EMAIL, InviteServiceRequest.FIELD_TELEPHONE_NUMBER, InviteServiceRequest.FIELD_PASSWORD);
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }

        String email = payload.get(InviteServiceRequest.FIELD_EMAIL).asText();

        if (!isValid(email)) {
            return Optional.of(Errors.from(format("Field [%s] must be a valid email address", InviteServiceRequest.FIELD_EMAIL)));
        }

        if (!isPublicSectorEmail(email)) {
            throw AdminUsersExceptions.invalidPublicSectorEmail(email);
        }

        String telephoneNumber = payload.get(InviteServiceRequest.FIELD_TELEPHONE_NUMBER).asText();
        if (!TelephoneNumberUtility.isValidPhoneNumber(telephoneNumber)) {
            return Optional.of(Errors.from(format("Field [%s] must be a valid telephone number", InviteServiceRequest.FIELD_TELEPHONE_NUMBER)));
        }

        return Optional.empty();

    }
}
