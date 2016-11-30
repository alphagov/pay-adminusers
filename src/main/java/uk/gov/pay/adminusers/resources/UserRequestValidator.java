package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.pay.adminusers.utils.Errors;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.math.NumberUtils.isDigits;

public class UserRequestValidator {

    public Optional<Errors> validateCreateRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = checkIfExists(payload, "username", "password", "email", "gatewayAccountId", "telephoneNumber", "otpKey");
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }
        Optional<List<String>> invalidData = checkIsNumeric(payload, "gatewayAccountId", "telephoneNumber");
        if (invalidData.isPresent()) {
            return Optional.of(Errors.from(invalidData.get()));
        }
        return Optional.empty();
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
}
