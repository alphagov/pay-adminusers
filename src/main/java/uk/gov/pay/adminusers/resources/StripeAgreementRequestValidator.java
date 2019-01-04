package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.List;
import java.util.Optional;

import static uk.gov.pay.adminusers.model.StripeAgreement.FIELD_IP_ADDRESS;

public class StripeAgreementRequestValidator {

    private final RequestValidations requestValidations;

    @Inject
    public StripeAgreementRequestValidator(RequestValidations requestValidations) {
        this.requestValidations = requestValidations;
    }

    public Optional<Errors> validateCreateRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkExistsAndNotEmpty(payload, FIELD_IP_ADDRESS);
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }
        Optional<List<String>> invalidType = requestValidations.checkIsString("Field [%s] must be a valid IP address", payload, FIELD_IP_ADDRESS);
        if (invalidType.isPresent()) {
            return Optional.of(Errors.from(invalidType.get()));
        }
        Optional<List<String>> invalidFormat = requestValidations.checkIsValidIpAddressFormat(payload, FIELD_IP_ADDRESS);
        return invalidFormat.map(Errors::from);
    }
}
