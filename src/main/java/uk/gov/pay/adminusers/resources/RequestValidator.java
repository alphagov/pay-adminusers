package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.math.NumberUtils.isDigits;

public class RequestValidator {

    Optional<List<String>> checkIsValid(JsonNode payload, Map<String, String> requiredData) {
        List<String> errors = newArrayList();
        errors.addAll(
                requiredData.keySet().stream()
                        .filter(fieldName -> !requiredData.get(fieldName).equals(payload.get(fieldName).asText()))
                        .map(fieldName -> format("Field [%s] must have value of [%s]", fieldName, requiredData.get(fieldName)))
                        .collect(Collectors.toList())
        );
        return Optional.of(errors);
    }

    Optional<List<String>> checkIsNumeric(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, isNotNumeric(), fieldNames, "Field [%s] must be a number");
    }

    Optional<List<String>> checkIfExists(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, notExist(), fieldNames, "Field [%s] is required");
    }

    Optional<List<String>> applyCheck(JsonNode payload, Function<JsonNode, Boolean> check, String[] fieldNames, String errorMessage) {
        List<String> errors = newArrayList();
        for (String fieldName : fieldNames) {
            if (check.apply(payload.get(fieldName))) {
                errors.add(format(errorMessage, fieldName));
            }
        }
        return errors.size() > 0 ? Optional.of(errors) : Optional.empty();
    }

    Function<JsonNode, Boolean> notExist() {
        return jsonElement -> (jsonElement == null || isBlank(jsonElement.asText()));
    }

    Function<JsonNode, Boolean> isNotNumeric() {
        return jsonNode -> !isDigits(jsonNode.asText());
    }
}
