package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.List;
import java.util.Optional;

import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_PASSWORD;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_TELEPHONE_NUMBER;
import static uk.gov.pay.adminusers.model.InviteRequest.*;

public class InviteRequestValidator {

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
        Optional<List<String>> missingMandatoryFields = requestValidations.checkIfExists(payload, FIELD_TELEPHONE_NUMBER, FIELD_PASSWORD);
        return missingMandatoryFields.map(Errors::from);
    }
}
