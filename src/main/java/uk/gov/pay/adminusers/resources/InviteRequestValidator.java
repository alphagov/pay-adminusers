package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import uk.gov.pay.adminusers.model.InviteValidateOtpRequest;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.List;
import java.util.Optional;

import static uk.gov.pay.adminusers.model.InviteOtpRequest.*;
import static uk.gov.pay.adminusers.model.InviteRequest.*;
import static uk.gov.pay.adminusers.model.InviteValidateOtpRequest.FIELD_OTP;

public class InviteRequestValidator {

    private static final int MAX_LENGTH_CODE = 255;
    private final RequestValidations requestValidations;

    @Inject
    public InviteRequestValidator(RequestValidations requestValidations) {
        this.requestValidations = requestValidations;
    }

    public Optional<Errors> validateCreateRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkIfExists(payload, FIELD_EMAIL, FIELD_ROLE_NAME, FIELD_SENDER);
        return missingMandatoryFields.map(Errors::from);
    }

    public Optional<Errors> validateOtpRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkIfExists(payload, FIELD_CODE, FIELD_TELEPHONE_NUMBER, FIELD_PASSWORD);
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }
        Optional<List<String>> invalidLength = requestValidations.checkMaxLength(payload, MAX_LENGTH_CODE, FIELD_CODE);
        return invalidLength.map(Errors::from);
    }

    public Optional<Errors> validateOtpValidationRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkIfExists(payload, InviteValidateOtpRequest.FIELD_CODE, FIELD_OTP);
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }
        Optional<List<String>> invalidLength = requestValidations.checkMaxLength(payload, MAX_LENGTH_CODE, InviteValidateOtpRequest.FIELD_CODE);
        return invalidLength.map(Errors::from);
    }
}
