package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import uk.gov.pay.adminusers.persistence.entity.service.SupportedLanguage;
import uk.gov.pay.adminusers.validations.RequestValidations;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_OP;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_PATH;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_VALUE;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_CUSTOM_BRANDING;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_GATEWAY_ACCOUNT_IDS;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_NAME;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_SERVICE_NAME_PREFIX;

public class ServiceUpdateOperationValidator {

    private static final String REPLACE = "replace";
    private static final String ADD = "add";

    private static final int SERVICE_NAME_MAX_LENGTH = 50;

    private final Map<String, List<String>> validAttributeUpdateOperations;
    
    private final RequestValidations requestValidations;

    @Inject
    public ServiceUpdateOperationValidator(RequestValidations requestValidations) {
        ImmutableMap.Builder<String, List<String>> validAttributeUpdateOperations = ImmutableMap.builder();
        validAttributeUpdateOperations.put(FIELD_NAME, singletonList(REPLACE));
        validAttributeUpdateOperations.put(FIELD_GATEWAY_ACCOUNT_IDS, singletonList(ADD));
        validAttributeUpdateOperations.put(FIELD_CUSTOM_BRANDING, singletonList(REPLACE));
        Arrays.stream(SupportedLanguage.values()).forEach(lang ->
                validAttributeUpdateOperations.put(FIELD_SERVICE_NAME_PREFIX + '/' + lang.toString(), singletonList(REPLACE)));
        this.validAttributeUpdateOperations = validAttributeUpdateOperations.build();
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
        } else if (FIELD_NAME.equals(path) || path.startsWith(FIELD_SERVICE_NAME_PREFIX)) {
            requestValidations.checkIfExistsOrEmpty(operation, FIELD_VALUE).ifPresent(errors::addAll);
            if (errors.isEmpty()) {
                requestValidations.checkMaxLength(operation, SERVICE_NAME_MAX_LENGTH, FIELD_VALUE).ifPresent(errors::addAll);
            }
        }

        return errors;
    }

    private List<String> validateOperationIsValidForPath(JsonNode operation) {
        String path = operation.get(FIELD_PATH).asText();

        if (!validAttributeUpdateOperations.keySet().contains(path)) {
            return singletonList(format("Path [%s] is invalid", path));
        }

        String op = operation.get("op").asText();
        if (!validAttributeUpdateOperations.get(path).contains(op)) {
            return singletonList(format("Operation [%s] is invalid for path [%s]", op, path));
        }

        return Collections.emptyList();
    }

    private static List<String> checkIfValidJson(JsonNode payload, String fieldName) {
        if (payload == null || !payload.isObject()) {
            return singletonList(format("Value for path [%s] must be a JSON", fieldName));
        }
        return Collections.emptyList();
    }

}
