package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.math.NumberUtils;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static uk.gov.pay.adminusers.model.Service.FIELD_SERVICE_NAME;


public class ServiceRequestValidator {

    private final RequestValidations requestValidations;

    @Inject
    public ServiceRequestValidator(RequestValidations requestValidations) {
        this.requestValidations = requestValidations;
    }



    public Optional<Errors> validateCreateRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkIfExists(payload, FIELD_SERVICE_NAME);
        if(missingMandatoryFields.isPresent()) {
            return missingMandatoryFields.map(Errors::from);
        }

        if (!requestValidations.notExistOrEmptyArray().apply(payload.get("gateway_account_ids"))) {
            if(nonNumericGatewayAccountIds(payload.get("gateway_account_ids"))) {
                return Optional.of(Errors.from("Field [gateway_account_ids] must contain numeric values"));
            }
        }


        return Optional.empty();

    }

    private boolean nonNumericGatewayAccountIds(JsonNode gatewayAccountNode) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<String> accountIds = mapper.readValue(gatewayAccountNode.asText(), new TypeReference<List<String>>(){});
            return accountIds.stream().filter(idString -> !NumberUtils.isDigits(idString)).count() > 0;
        } catch (IOException e) {
            return true;
        }
    }
}
