package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static uk.gov.pay.adminusers.model.Service.FIELD_SERVICE_NAME;


public class ServiceRequestValidator {

    private final RequestValidations requestValidations;

    @Inject
    public ServiceRequestValidator(RequestValidations requestValidations) {
        this.requestValidations = requestValidations;
    }

    public Optional<Errors> validateCreateRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkIfExists(payload, FIELD_SERVICE_NAME);
        if(missingMandatoryFields.isPresent()) {
            return missingMandatoryFields.map(Errors::from);
        }

        return Optional.empty();

    }
}
