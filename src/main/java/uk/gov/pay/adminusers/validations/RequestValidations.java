package uk.gov.pay.adminusers.validations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import uk.gov.pay.adminusers.utils.email.EmailValidator;
import uk.gov.pay.adminusers.utils.telephonenumber.TelephoneNumberUtility;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.math.NumberUtils.isDigits;

public class RequestValidations {

    public Optional<List<String>> checkIsNumeric(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, isNotNumeric(), fieldNames, "Field [%s] must be a number");
    }

    public Optional<List<String>> checkIsBoolean(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, isNotBoolean(), fieldNames, "Field [%s] must be a boolean");
    }

    public Optional<List<String>> checkIsStrictBoolean(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, isNotStrictBoolean(), fieldNames, "Field [%s] must be a boolean");
    }

    public Optional<List<String>> checkExistsAndNotEmpty(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, notExistsOrIsEmpty(), fieldNames, "Field [%s] is required");
    }

    public Optional<List<String>> checkExists(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, notExists(), fieldNames, "Field [%s] is required");
    }

    public Optional<List<String>> checkMaxLength(JsonNode payload, int maxLength, String... fieldNames) {
        return applyCheck(payload, exceedsMaxLength(maxLength), fieldNames, "Field [%s] must have a maximum length of " + maxLength + " characters");
    }

    public Optional<List<String>> checkIsString(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, isNotString(), fieldNames, "Field [%s] must be a string");
    }

    public Optional<List<String>> checkIsString(String errorMsg, JsonNode payload, String... fieldNames) {
        return applyCheck(payload, isNotString(), fieldNames, errorMsg);
    }

    public Optional<List<String>> checkIsValidTelephoneNumber(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, isNotValidTelephoneNumber(), fieldNames, "Field [%s] must be a valid telephone number");
    }

    public Optional<List<String>> checkIsZonedDateTime(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, isNotZonedDateTime(), fieldNames, "Field [%s] must be a valid date time with timezone");
    }

    private Function<JsonNode, Boolean> exceedsMaxLength(int maxLength) {
        return jsonNode -> jsonNode.asText().length() > maxLength;
    }

    public Optional<List<String>> applyCheck(JsonNode payload, Function<JsonNode, Boolean> check, String[] fieldNames, String errorMessage) {
        List<String> errors = new ArrayList<>();
        for (String fieldName : fieldNames) {
            if (check.apply(payload.get(fieldName))) {
                errors.add(format(errorMessage, fieldName));
            }
        }
        return errors.isEmpty() ? Optional.empty() : Optional.of(errors);
    }

    public Optional<List<String>> isValidEnumValue(JsonNode payload, EnumSet<?> enumSet, String field) {
        String value = payload.get(field).asText();
        if (enumSet.stream().noneMatch(constant -> constant.name().equals(value))) {
            return Optional.of(singletonList(format("Field [%s] must be one of %s", field, enumSet)));
        }
        return Optional.empty();
    }

    private Function<JsonNode, Boolean> notExistsOrIsEmpty() {
        return (JsonNode jsonElement) -> {
            if (jsonElement instanceof NullNode) {
                return isNullValue().apply(jsonElement);
            } else if (jsonElement instanceof ArrayNode) {
                return notExistOrEmptyArray().apply(jsonElement);
            } else {
                return notExistOrBlankText().apply(jsonElement);
            }
        };
    }

    private Function<JsonNode, Boolean> notExists() {
        return (JsonNode jsonElement) -> isNullValue().apply(jsonElement);
    }

    private Function<JsonNode, Boolean> notExistOrEmptyArray() {
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

    static Function<JsonNode, Boolean> isNotNumeric() {
        return jsonNode -> !isDigits(jsonNode.asText());
    }

    static Function<JsonNode, Boolean> isNotBoolean() {
        return jsonNode -> !List.of("true", "false").contains(jsonNode.asText().toLowerCase(Locale.ENGLISH));
    }

    private static Function<JsonNode, Boolean> isNotStrictBoolean() {
        return jsonNode -> !jsonNode.isBoolean();
    }

    private static Function<JsonNode, Boolean> isNotString() {
        return jsonNode -> !jsonNode.isTextual();
    }

    static Function<JsonNode, Boolean> isNotValidTelephoneNumber() {
        return jsonNode -> !TelephoneNumberUtility.isValidPhoneNumber(jsonNode.asText());
    }

    static Function<JsonNode, Boolean> isNotAValidTimeZone() {
        return jsonNode -> {
            try {
                ZoneId.of(jsonNode.asText());
                return false;
            } catch (DateTimeException e) {
                return true;
            }
        };
    }

    static Function<JsonNode, Boolean> isNotValidEmail() {
        return jsonNode -> !EmailValidator.isValid(jsonNode.asText());
    }

    private static Function<JsonNode, Boolean> isNotZonedDateTime() {
        return jsonNode -> !jsonNode.isTextual() || !isZonedDateTime(jsonNode.textValue());
    }

    private static boolean isZonedDateTime(String value) {
        try {
            ZonedDateTime.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public Optional<List<String>> isValidEmail(JsonNode payload, String... fieldNames) {
        return applyCheck(
                payload,
                jsonNode -> !EmailValidator.isValid(jsonNode.asText()),
                fieldNames,
                "Field [email] must be a valid email address");
    }
}
