package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import uk.gov.pay.adminusers.validations.RequestValidations;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_OP;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_PATH;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_VALUE;

public class ServiceUpdateOperationValidator {
    
    static final String FIELD_NAME = "name";
    static final String FIELD_GATEWAY_ACCOUNT_IDS = "gateway_account_ids";
    static final String FIELD_CUSTOM_BRANDING = "custom_branding";
    static final String FIELD_SERVICE_NAME = "service_name";
    
    private static final String REPLACE = "replace";
    private static final String ADD = "add";
    
    private static final int SERVICE_NAME_MAX_LENGTH = 50;
    
    private static final Map<String, List<String>> VALID_ATTRIBUTE_UPDATE_OPERATIONS = ImmutableMap.of(
            FIELD_NAME, Collections.singletonList(REPLACE),
            FIELD_GATEWAY_ACCOUNT_IDS, Collections.singletonList(ADD),
            FIELD_CUSTOM_BRANDING, Collections.singletonList(REPLACE),
            FIELD_SERVICE_NAME, Collections.singletonList(REPLACE));

    private final RequestValidations requestValidations;

    @Inject
    public ServiceUpdateOperationValidator(RequestValidations requestValidations) {
        this.requestValidations = requestValidations;
    }

    List<String> validate(JsonNode operation) {
        List<String> errors = validateOpAndPathExistAndNotEmpty(operation);
        if (!errors.isEmpty()) {
            return errors;
        }

        errors = validateValueIsValidForPath(operation);
        if (!errors.isEmpty()) {
            return errors;
        }

        errors = validateOperationIsValidForPath(operation);
        if (!errors.isEmpty()) {
            return errors;
        }

        return Collections.emptyList();
    }

    private List<String> validateOpAndPathExistAndNotEmpty(JsonNode operation) {
        List<String> errors = new ArrayList<>();
        requestValidations.checkIfExistsOrEmpty(operation, FIELD_OP, FIELD_PATH).ifPresent(errors::addAll);
        return errors;
    }

    private List<String> validateValueIsValidForPath(JsonNode operation) {
        List<String> errors = new ArrayList<>();

        String path = operation.get(FIELD_PATH).asText();

        if (FIELD_CUSTOM_BRANDING.equals(path)) {
            errors.addAll(checkIfValidJson(operation.get(FIELD_VALUE), FIELD_CUSTOM_BRANDING));
        } else if (FIELD_NAME.equals(path)) {
            requestValidations.checkIfExistsOrEmpty(operation, FIELD_VALUE).ifPresent(errors::addAll);
            if (errors.isEmpty()) {
                requestValidations.checkMaxLength(operation, SERVICE_NAME_MAX_LENGTH, FIELD_VALUE).ifPresent(errors::addAll);
            }
        }

        return errors;
    }

    private static List<String> validateOperationIsValidForPath(JsonNode operation) {
        String path = operation.get(FIELD_PATH).asText();

        if (!VALID_ATTRIBUTE_UPDATE_OPERATIONS.keySet().contains(path)) {
            return Collections.singletonList(format("Path [%s] is invalid", path));
        }

        String op = operation.get("op").asText();
        if (!VALID_ATTRIBUTE_UPDATE_OPERATIONS.get(path).contains(op)) {
            return Collections.singletonList(format("Operation [%s] is invalid for path [%s]", op, path));
        }

        return Collections.emptyList();
    }

    private static List<String> checkIfValidJson(JsonNode payload, String fieldName) {
        if (payload == null || !payload.isObject()) {
            return Collections.singletonList(format("Value for path [%s] must be a JSON", fieldName));
        }
        return Collections.emptyList();
    }

}
