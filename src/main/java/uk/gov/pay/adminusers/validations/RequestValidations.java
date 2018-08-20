package uk.gov.pay.adminusers.validations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.collect.ImmutableList;
import uk.gov.pay.adminusers.utils.email.EmailValidator;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.math.NumberUtils.isDigits;

public class RequestValidations {

    public Optional<List<String>> checkIsNumeric(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, isNotNumeric(), fieldNames, "Field [%s] must be a number");
    }

    public Optional<List<String>> checkIsBoolean(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, isNotBoolean(), fieldNames, "Field [%s] must be a boolean");
    }

    public Optional<List<String>> checkIfExistsOrEmpty(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, notExistOrEmpty(), fieldNames, "Field [%s] is required");
    }

    public List<String> checkIfExistsOrEmptyV2(JsonNode payload, String... fieldNames) {
        return applyCheckV2(payload, notExistOrEmpty(), fieldNames, "Field [%s] is required");
    }

    public Optional<List<String>> checkMaxLength(JsonNode payload, int maxLength, String... fieldNames) {
        return applyCheck(payload, exceedsMaxLength(maxLength), fieldNames, "Field [%s] must have a maximum length of " + maxLength + " characters");
    }

    public List<String> checkMaxLengthV2(JsonNode payload, int maxLength, String... fieldNames) {
        return applyCheckV2(payload, exceedsMaxLength(maxLength), fieldNames, "Field [%s] must have a maximum length of " + maxLength + " characters");
    }

    private Function<JsonNode, Boolean> exceedsMaxLength(int maxLength) {
        return jsonNode -> jsonNode.asText().length() > maxLength;
    }

    public Optional<List<String>> applyCheck(JsonNode payload, Function<JsonNode, Boolean> check, String[] fieldNames, String errorMessage) {
        List<String> errors = newArrayList();
        for (String fieldName : fieldNames) {
            if (check.apply(payload.get(fieldName))) {
                errors.add(format(errorMessage, fieldName));
            }
        }
        return errors.isEmpty() ? Optional.empty() : Optional.of(errors);
    }

    private List<String> applyCheckV2(JsonNode payload, Function<JsonNode, Boolean> check, String[] fieldNames, String errorMessage) {
        List<String> errors = newArrayList();
        for (String fieldName : fieldNames) {
            if (check.apply(payload.get(fieldName))) {
                errors.add(format(errorMessage, fieldName));
            }
        }
        return errors;
    }

    public Function<JsonNode, Boolean> notExistOrEmpty() {
        return (jsonElement) -> {
            if (jsonElement instanceof NullNode) {
                return isNullValue().apply(jsonElement);
            } else if (jsonElement instanceof ArrayNode) {
                return notExistOrEmptyArray().apply(jsonElement);
            } else {
                return notExistOrBlankText().apply(jsonElement);
            }
        };
    }

    public Function<JsonNode, Boolean> notExistOrEmptyArray() {
        return jsonElement -> (
                jsonElement == null ||
                        ((jsonElement instanceof ArrayNode) && (jsonElement.size() == 0))
        );
    }

    private static Function<JsonNode, Boolean> notExistOrBlankText() {
        return jsonElement -> (
                jsonElement == null ||
                        isBlank(jsonElement.asText())
        );
    }

    private static Function<JsonNode, Boolean> isNullValue() {
        return jsonElement -> (
                jsonElement == null || jsonElement instanceof NullNode
        );
    }

    public static Function<JsonNode, Boolean> isNotNumeric() {
        return jsonNode -> !isDigits(jsonNode.asText());
    }

    public static Function<JsonNode, Boolean> isNotBoolean() {
        return jsonNode -> !ImmutableList.of("true", "false").contains(jsonNode.asText().toLowerCase());
    }

    public Optional<List<String>> isValidEmail(JsonNode payload, String... fieldNames) {
        return applyCheck(
                payload,
                jsonNode -> !EmailValidator.isValid(jsonNode.asText()),
                fieldNames,
                "Field [email] must be a valid email address");
    }
}
