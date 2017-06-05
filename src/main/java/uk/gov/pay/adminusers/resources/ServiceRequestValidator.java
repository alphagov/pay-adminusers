package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.math.NumberUtils;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;


public class ServiceRequestValidator {

    public static final String FIELD_SERVICE_NAME = "name";
    public static final String FIELD_GATEWAY_ACCOUNT_IDS = "gateway_account_ids";
    private final RequestValidations requestValidations;

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
                return Optional.of(Errors.from(String.format("Field [%s] must contain numeric values", FIELD_GATEWAY_ACCOUNT_IDS)));
            }
        }
        return Optional.empty();
    }

    private boolean nonNumericGatewayAccountIds(JsonNode gatewayAccountNode) {
        List<JsonNode> accountIds = Lists.newArrayList(gatewayAccountNode.elements());
        return accountIds.stream().filter(idNode -> !NumberUtils.isDigits(idNode.asText())).count() > 0;
    }
}
