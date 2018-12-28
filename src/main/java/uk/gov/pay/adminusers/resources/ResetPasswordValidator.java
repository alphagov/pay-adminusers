package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.collect.ImmutableList.of;
import static uk.gov.pay.adminusers.resources.ResetPasswordResource.FIELD_CODE;
import static uk.gov.pay.adminusers.resources.ResetPasswordResource.FIELD_PASSWORD;

public class ResetPasswordValidator {

    private final RequestValidations requestValidations;

    @Inject
    public ResetPasswordValidator(RequestValidations requestValidations) {
        this.requestValidations = requestValidations;
    }

    public Optional<Errors> validateResetRequest(JsonNode payload) {

        if (payload == null) {
            return Optional.of(Errors.from(of("invalid JSON")));
        }

        Optional<List<String>> missingMandatoryFields = requestValidations.checkExistsAndNotEmpty(payload, FIELD_CODE, FIELD_PASSWORD);

        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }

        Optional<List<String>> invalidLength = checkLength(payload, FIELD_CODE);

        return invalidLength.map(Errors::from);

    }

    private Optional<List<String>> checkLength(JsonNode payload, String... fieldNames) {
        return requestValidations.applyCheck(payload, exceedsMaxLength(), fieldNames, "Field [%s] must have a maximum length of 255 characters");
    }

    private Function<JsonNode, Boolean> exceedsMaxLength() {
        return jsonNode -> jsonNode.asText().length() > 255;
    }
}
