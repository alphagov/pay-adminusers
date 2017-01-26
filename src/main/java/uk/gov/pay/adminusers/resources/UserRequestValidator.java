package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.pay.adminusers.utils.Errors;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.math.NumberUtils.isDigits;
import static uk.gov.pay.adminusers.model.User.*;

public class UserRequestValidator {

    public Optional<Errors> validateAuthenticateRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = checkIfExists(payload, FIELD_USERNAME, FIELD_PASSWORD);
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }
        return Optional.empty();
    }

    public Optional<Errors> validateCreateRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = checkIfExists(payload, FIELD_USERNAME, FIELD_EMAIL, FIELD_GATEWAY_ACCOUNT_ID, FIELD_TELEPHONE_NUMBER, FIELD_ROLE_NAME);
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }
        Optional<List<String>> invalidData = checkIsNumeric(payload, FIELD_GATEWAY_ACCOUNT_ID, FIELD_TELEPHONE_NUMBER);
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
        return applyCheck(payload, exceedsMaxLength(), fieldNames, "Field [%s] must have a maximum length of 255 characters");
    }

    private Function<JsonNode, Boolean> exceedsMaxLength() {
        return jsonNode -> jsonNode.asText().length() > 255;
    }

    public Optional<Errors> validatePatchRequest(JsonNode payload, Map<String, String> requiredData) {
        Optional<List<String>> missingMandatoryFields = checkIfExists(payload, "op", "path", "value" );
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }

        Optional<List<String>> invalidData = checkIsValid(payload, requiredData);
        if (invalidData.isPresent()) {
            return Optional.of(Errors.from(invalidData.get()));
        }

        return Optional.empty();
    }

    private Optional<List<String>> checkIsValid(JsonNode payload, Map<String, String> requiredData) {
        List<String> errors = newArrayList();
        errors.addAll(
                requiredData.keySet().stream()
                        .filter(fieldName -> !requiredData.get(fieldName).equals(payload.get(fieldName).asText()))
                        .map(fieldName -> format("Field [%s] must have value of [%s]", fieldName, requiredData.get(fieldName)))
                        .collect(Collectors.toList())
        );
        return Optional.of(errors);
    }

    private Optional<List<String>> checkIsNumeric(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, isNotNumeric(), fieldNames, "Field [%s] must be a number");
    }

    private Optional<List<String>> checkIfExists(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, notExist(), fieldNames, "Field [%s] is required");
    }

    private Optional<List<String>> applyCheck(JsonNode payload, Function<JsonNode, Boolean> check, String[] fieldNames, String errorMessage) {
        List<String> errors = newArrayList();
        for (String filedName : fieldNames) {
            if (check.apply(payload.get(filedName))) {
                errors.add(format(errorMessage, filedName));
            }
        }
        return errors.size() > 0 ? Optional.of(errors) : Optional.empty();
    }

    private Function<JsonNode, Boolean> notExist() {
        return jsonElement -> (jsonElement == null || isBlank(jsonElement.asText()));
    }

    private Function<JsonNode, Boolean> isNotNumeric() {
        return jsonNode -> !isDigits(jsonNode.asText());
    }

    public Optional<Errors> valueIsNumeric(JsonNode payload, Map<String, String> requiredData) {
        Optional<Errors> invalidPatch = validatePatchRequest(payload, requiredData);
        if (invalidPatch.isPresent() && invalidPatch.get().getErrors().size() > 0) {
            return invalidPatch;
        }

        Optional<List<String>> numericErrors = checkIsNumeric(payload, "value");
        if (numericErrors.isPresent()) {
            return Optional.of(Errors.from(numericErrors.get()));
        }
        return Optional.empty();
    }
}
