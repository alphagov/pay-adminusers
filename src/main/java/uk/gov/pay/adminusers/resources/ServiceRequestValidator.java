package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.collect.ImmutableList.of;
import static uk.gov.pay.adminusers.resources.ServiceResource.FIELD_NEW_SERVICE_NAME;

public class ServiceRequestValidator {

    private final RequestValidations requestValidations;

    @Inject
    public ServiceRequestValidator(RequestValidations requestValidations) {
        this.requestValidations = requestValidations;
    }

    public Optional<Errors> validateUpdateRequest(JsonNode payload) {

        if (payload == null) {
            return Optional.of(Errors.from(of("invalid JSON")));
        }

        Optional<List<String>> missingMandatoryFields = requestValidations.checkIfExists(payload, FIELD_NEW_SERVICE_NAME);

        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }

        Optional<List<String>> invalidLength = checkLength(payload, FIELD_NEW_SERVICE_NAME);

        if (invalidLength.isPresent()) {
            return Optional.of(Errors.from(invalidLength.get()));
        }

        return Optional.empty();
    }

    private Optional<List<String>> checkLength(JsonNode payload, String... fieldNames) {
        return requestValidations.applyCheck(payload, exceedsMaxLength(), fieldNames, "Field [%s] must have a maximum length of 50 characters");
    }

    private Function<JsonNode, Boolean> exceedsMaxLength() {
        return jsonNode -> jsonNode.asText().length() > 50;
    }

}
