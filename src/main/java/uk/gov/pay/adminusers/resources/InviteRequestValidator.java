package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.List;
import java.util.Optional;

import static uk.gov.pay.adminusers.model.InviteRequest.*;
import static uk.gov.pay.adminusers.model.InviteRequest.FIELD_ROLE_NAME;
import static uk.gov.pay.adminusers.model.InviteRequest.FIELD_SENDER;

public class InviteRequestValidator {

    private final RequestValidations requestValidations;

    @Inject
    public InviteRequestValidator(RequestValidations requestValidations) {
        this.requestValidations = requestValidations;
    }

    public Optional<Errors> validateCreateRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkIfExists(payload, FIELD_EMAIL, FIELD_ROLE_NAME, FIELD_SENDER);
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }
        return Optional.empty();
    }
}
