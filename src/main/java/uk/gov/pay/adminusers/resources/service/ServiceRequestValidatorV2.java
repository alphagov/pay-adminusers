package uk.gov.pay.adminusers.resources.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.resources.ServiceRequestValidator;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_OP;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_PATH;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_VALUE;

public class ServiceRequestValidatorV2 extends ServiceRequestValidator {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(ServiceRequestValidatorV2.class);

    public static final String FIELD_SERVICE_SERVICE_NAME = "service_name";
    private static final String STRING_REPLACE = "replace";
    private static final int SERVICE_NAME_MAX_LENGTH = 50;
    private static final String STRING_ADD = "add";
    private static final Map<String, List<String>> VALID_ATTRIBUTE_UPDATE_OPERATIONS = ImmutableMap.of(
            FIELD_NAME, Collections.singletonList(STRING_REPLACE),
            FIELD_GATEWAY_ACCOUNT_IDS, Collections.singletonList(STRING_ADD),
            FIELD_CUSTOM_BRANDING, Collections.singletonList(STRING_REPLACE),
            FIELD_SERVICE_SERVICE_NAME, Collections.singletonList(STRING_REPLACE));

    private final RequestValidations requestValidations;

    @Inject
    public ServiceRequestValidatorV2(RequestValidations requestValidations) {
        super(requestValidations);
        this.requestValidations = requestValidations;
    }


    @Override
    public Optional<Errors> validateUpdateAttributeRequest(JsonNode payload) {
        List<String> finalErrors = new ArrayList<>();
        ArrayNode operations = new ObjectMapper().createArrayNode();
        try {
            operations = (ArrayNode) new ObjectMapper().readTree(payload.toString());
        } catch (IOException e) {
            LOGGER.info("There was an exception processing update request [{}]", e.getMessage());
            finalErrors.add("There was an error processing update");
        } catch (ClassCastException e) {
            LOGGER.info("There was an exception processing update request [{}]", e.getMessage());
            finalErrors.add("The JSON payload needs to be an array");
        }

        if (!finalErrors.isEmpty()) {
            return Optional.of(Errors.from(finalErrors));
        }

        operations.forEach(item ->
                finalErrors.addAll(requestValidations.checkIfExistsOrEmptyV2(item, FIELD_OP, FIELD_PATH)));

        if (!finalErrors.isEmpty()) {
            return Optional.of(Errors.from(finalErrors));
        }
        operations.forEach(item -> {
            String path = item.get("path").asText();

            if (FIELD_CUSTOM_BRANDING.equals(path)) {
                finalErrors.addAll(checkIfValidJson(item.get(FIELD_VALUE)));
            } else if (FIELD_NAME.equals(path)) {
                finalErrors.addAll(requestValidations.checkIfExistsOrEmptyV2(item, FIELD_VALUE));
                if (finalErrors.isEmpty()) {
                    finalErrors.addAll(requestValidations.checkMaxLengthV2(item, SERVICE_NAME_MAX_LENGTH, FIELD_VALUE));
                }
            }
        });
        if (!finalErrors.isEmpty()) {
            return Optional.of(Errors.from(finalErrors));
        }
        operations.forEach(item -> {
            String path = item.get("path").asText();

            if (!VALID_ATTRIBUTE_UPDATE_OPERATIONS.keySet().contains(path)) {
                finalErrors.add(format("Path [%s] is invalid", path));
                return;
            }

            String op = item.get("op").asText();
            if (!VALID_ATTRIBUTE_UPDATE_OPERATIONS.get(path).contains(op)) {
                finalErrors.add(format("Operation [%s] is invalid for path [%s]", op, path));
            }
        });

        if (!finalErrors.isEmpty()) {
            return Optional.of(Errors.from(finalErrors));
        }
        return Optional.empty();
    }

    private List<String> checkIfValidJson(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            return Collections.singletonList(format("Value for path [%s] must be a JSON", FIELD_CUSTOM_BRANDING));
        }
        return Collections.emptyList();
    }
}
