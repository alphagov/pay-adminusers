package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.List;
import java.util.Optional;

public class GovUkPayAgreementRequestValidator {

    private static final String FIELD_USER_ID = "user_external_id";
    private final RequestValidations requestValidations;

    @Inject
    public GovUkPayAgreementRequestValidator(RequestValidations requestValidations) {
        this.requestValidations = requestValidations;
    }
    
    public Optional<Errors> validateCreateRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkExistsAndNotEmpty(payload, FIELD_USER_ID);
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }
        Optional<List<String>> invalidType = requestValidations.checkIsString("Field [%s] must be a valid user ID", payload, FIELD_USER_ID);
        if (invalidType.isPresent()) {
            return Optional.of(Errors.from(invalidType.get()));
        }
        return Optional.empty();
    }
}
