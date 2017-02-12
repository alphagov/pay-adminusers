package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import jersey.repackaged.com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static uk.gov.pay.adminusers.model.User.*;
import static uk.gov.pay.adminusers.validations.UserPatchValidations.*;

public class UserRequestValidator {

    private final RequestValidations requestValidations;

    @Inject
    public UserRequestValidator(RequestValidations requestValidations) {
        this.requestValidations = requestValidations;
    }

    public Optional<Errors> validateAuthenticateRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkIfExists(payload, FIELD_USERNAME, FIELD_PASSWORD);
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }
        return Optional.empty();
    }

    public Optional<Errors> validateCreateRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkIfExists(payload, FIELD_USERNAME, FIELD_EMAIL, FIELD_GATEWAY_ACCOUNT_ID, FIELD_TELEPHONE_NUMBER, FIELD_ROLE_NAME);
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }
        Optional<List<String>> invalidData = requestValidations.checkIsNumeric(payload, FIELD_GATEWAY_ACCOUNT_ID, FIELD_TELEPHONE_NUMBER);
        if (invalidData.isPresent()) {
            return Optional.of(Errors.from(invalidData.get()));
        }
        Optional<List<String>> invalidLength = checkLength(payload, FIELD_USERNAME);
        if (invalidLength.isPresent()) {
            return Optional.of(Errors.from(invalidLength.get()));
        }
        return Optional.empty();
    }

    private Optional<List<String>> checkLength(JsonNode payload, String... fieldNames) {
        return requestValidations.applyCheck(payload, exceedsMaxLength(), fieldNames, "Field [%s] must have a maximum length of 255 characters");
    }

    private Function<JsonNode, Boolean> exceedsMaxLength() {
        return jsonNode -> jsonNode.asText().length() > 255;
    }

    public Optional<Errors> validate2FAAuthRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkIfExists(payload, "code");
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }

        Optional<List<String>> notNumeric = requestValidations.checkIsNumeric(payload, "code");
        if(notNumeric.isPresent()){
            return Optional.of(Errors.from(notNumeric.get()));
        }
        return Optional.empty();
    }

    public Optional<Errors> validatePatchRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkIfExists(payload, "op", "path", "value");
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }

        String path = payload.get("path").asText();
        String op = payload.get("op").asText();

        if (!isPathAllowed(path)) {
            return Optional.of(Errors.from(ImmutableList.of(format("Patching path [%s] not allowed", path))));
        }

        if (!isAllowedOpForPath(path, op)) {
            return Optional.of(Errors.from(ImmutableList.of(format("Operation [%s] not allowed for path [%s]", op, path))));
        }

        Optional<List<String>> invalidData = checkValidPatchValue(payload.get("value"), getUserPatchPathValidations(path));
        if (invalidData.isPresent()) {
            return Optional.of(Errors.from(invalidData.get()));
        }

        return Optional.empty();
    }

    private Optional<List<String>> checkValidPatchValue(JsonNode valueNode, Collection<Pair<Function<JsonNode, Boolean>, String>> pathValidations) {
        List<String> errors = newArrayList();
        pathValidations.forEach(validationPair -> {
            if (validationPair.getLeft().apply(valueNode)) {
                errors.add(validationPair.getRight());
            }
        });
        return errors.size() !=0 ? Optional.of(errors) : Optional.empty() ;
    }
}
