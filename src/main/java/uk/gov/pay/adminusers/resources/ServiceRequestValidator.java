package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.math.NumberUtils;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Arrays.asList;


public class ServiceRequestValidator {

    public static final String FIELD_SERVICE_NAME = "name";
    public static final String FIELD_GATEWAY_ACCOUNT_IDS = "gateway_account_ids";
    private static final String FIELD_OP = "op";
    private static final String FIELD_PATH = "path";
    private static final String FIELD_VALUE = "value";
    private final RequestValidations requestValidations;
    private static final Map<String, List<String>> VALID_ATTRIBUTE_UPDATE_OPERATIONS = new HashMap<String, List<String>>(){{
        put(FIELD_SERVICE_NAME, asList("replace"));
        put(FIELD_GATEWAY_ACCOUNT_IDS, asList("add"));
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
        Optional<List<String>> errors = requestValidations.checkIfExists(payload, FIELD_OP, FIELD_PATH, FIELD_VALUE);

        if(errors.isPresent()) {
            return Optional.of(Errors.from(errors.get()));
        }
        String path = payload.get("path").asText();
        if (!VALID_ATTRIBUTE_UPDATE_OPERATIONS.keySet().contains(path)) {
            return Optional.of(Errors.from(format("Path [%s] is invalid", path)));
        }


        String op = payload.get("op").asText();

        if (!VALID_ATTRIBUTE_UPDATE_OPERATIONS.get(path).contains(op)) {
            return Optional.of(Errors.from(format("Operation [%s] is invalid for path [%s]", op, path)));
        }


        return Optional.empty();

    }
}
