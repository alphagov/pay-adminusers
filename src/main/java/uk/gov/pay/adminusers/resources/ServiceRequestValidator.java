package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.math.NumberUtils;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import javax.inject.Inject;
import java.util.*;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.*;


public class ServiceRequestValidator {

    public static final String FIELD_SERVICE_NAME = "name";
    public static final String FIELD_GATEWAY_ACCOUNT_IDS = "gateway_account_ids";
    public static final String FIELD_CUSTOM_BRANDING = "custom_branding";

    private final RequestValidations requestValidations;
    private static final Map<String, List<String>> VALID_ATTRIBUTE_UPDATE_OPERATIONS = new HashMap<String, List<String>>() {{
        put(FIELD_SERVICE_NAME, asList("replace"));
        put(FIELD_GATEWAY_ACCOUNT_IDS, asList("add"));
        put(FIELD_CUSTOM_BRANDING, asList("replace"));
    }};

    @Inject
    public ServiceRequestValidator(RequestValidations requestValidations) {
        this.requestValidations = requestValidations;
    }


    public Optional<Errors> validateCreateRequest(JsonNode payload) {
        if (payload == null || "{}".equals(payload.toString())) {
            return Optional.empty();
        }

        if (!requestValidations.notExistOrEmptyArray().apply(payload.get(FIELD_GATEWAY_ACCOUNT_IDS))) {
            if (nonNumericGatewayAccountIds(payload.get(FIELD_GATEWAY_ACCOUNT_IDS))) {
                return Optional.of(Errors.from(format("Field [%s] must contain numeric values", FIELD_GATEWAY_ACCOUNT_IDS)));
            }
        }
        return Optional.empty();
    }

    private boolean nonNumericGatewayAccountIds(JsonNode gatewayAccountNode) {
        List<JsonNode> accountIds = Lists.newArrayList(gatewayAccountNode.elements());
        return accountIds.stream().filter(idNode -> !NumberUtils.isDigits(idNode.asText())).count() > 0;
    }

    public Optional<Errors> validateUpdateAttributeRequest(JsonNode payload) {
        Optional<List<String>> errors = requestValidations.checkIfExists(payload, FIELD_OP, FIELD_PATH);
        if (errors.isPresent()) {
            return Optional.of(Errors.from(errors.get()));
        }

        String path = payload.get("path").asText();
        if (!FIELD_CUSTOM_BRANDING.equals(path)) {
            errors = requestValidations.checkIfExists(payload, FIELD_VALUE);
        } else {
            errors = checkIfNotEmptyAndJson(payload.get(FIELD_VALUE));
        }

        if (errors.isPresent()) {
            return Optional.of(Errors.from(errors.get()));
        }

        if (!VALID_ATTRIBUTE_UPDATE_OPERATIONS.keySet().contains(path)) {
            return Optional.of(Errors.from(format("Path [%s] is invalid", path)));
        }

        String op = payload.get("op").asText();
        if (!VALID_ATTRIBUTE_UPDATE_OPERATIONS.get(path).contains(op)) {
            return Optional.of(Errors.from(format("Operation [%s] is invalid for path [%s]", op, path)));
        }

        return Optional.empty();
    }

    private Optional<List<String>> checkIfNotEmptyAndJson(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            return Optional.of(Collections.singletonList(format("Value for path [%s] must be a JSON", FIELD_CUSTOM_BRANDING)));
        }
        return Optional.empty();
    }

    public Optional<Errors> validateFindRequest(String gatewayAccountId) {
        if (isBlank(gatewayAccountId)) {
            return Optional.of(Errors.from("Find services currently support only by gatewayAccountId"));
        }
        if (!isNumeric(gatewayAccountId)) {
            return Optional.of(Errors.from("Query param [gatewayAccountId] must be numeric"));
        }
        return Optional.empty();
    }

}
