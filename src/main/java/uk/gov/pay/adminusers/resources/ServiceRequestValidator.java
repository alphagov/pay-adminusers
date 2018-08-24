package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.exception.ValidationException;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_OP;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_PATH;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_VALUE;


public class ServiceRequestValidator {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(ServiceRequestValidator.class);

    public static final String FIELD_NAME = "name";
    public static final String FIELD_GATEWAY_ACCOUNT_IDS = "gateway_account_ids";
    public static final String FIELD_CUSTOM_BRANDING = "custom_branding";
    static final String FIELD_MERCHANT_DETAILS_NAME = "name";
    static final String FIELD_MERCHANT_DETAILS_ADDRESS_LINE1 = "address_line1";
    static final String FIELD_MERCHANT_DETAILS_ADDRESS_CITY = "address_city";
    static final String FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE = "address_postcode";
    static final String FIELD_MERCHANT_DETAILS_ADDRESS_COUNTRY = "address_country";
    static final String FIELD_MERCHANT_DETAILS_EMAIL = "email";
    public static final String FIELD_SERVICE_SERVICE_NAME = "service_name";
    private static final String STRING_REPLACE = "replace";
    private static final String STRING_ADD = "add";
    private static final int SERVICE_NAME_MAX_LENGTH = 50;
    private static final int FIELD_MERCHANT_DETAILS_EMAIL_MAX_LENGTH = 255;
    private static final Map<String, List<String>> VALID_ATTRIBUTE_UPDATE_OPERATIONS = ImmutableMap.of(
            FIELD_NAME, Collections.singletonList(STRING_REPLACE),
            FIELD_GATEWAY_ACCOUNT_IDS, Collections.singletonList(STRING_ADD),
            FIELD_CUSTOM_BRANDING, Collections.singletonList(STRING_REPLACE),
            FIELD_SERVICE_SERVICE_NAME, Collections.singletonList(STRING_REPLACE));

    private final RequestValidations requestValidations;

    @Inject
    public ServiceRequestValidator(RequestValidations requestValidations) {
        this.requestValidations = requestValidations;
    }

    Optional<Errors> validateUpdateAttributeRequest(JsonNode payload) {

        JsonNode operations;
        try {
            operations = new ObjectMapper().readTree(payload.toString());
            if (!operations.isArray()) {
                operations = new ObjectMapper().createArrayNode();
                ((ArrayNode) operations).add(payload);
            }
        } catch (IOException e) {
            LOGGER.error("There was an exception processing update request [{}]", e.getMessage());
            return Optional.of(Errors.from("There was an error processing update request"));
        }

        List<String> finalErrors = validateOpAndPathIfExistOrEmpty(operations);
        if (!finalErrors.isEmpty()) {
            return Optional.of(Errors.from(finalErrors));
        }
        finalErrors = validatePath(operations);
        if (!finalErrors.isEmpty()) {
            return Optional.of(Errors.from(finalErrors));
        }
        finalErrors = validateOperations(operations);

        if (!finalErrors.isEmpty()) {
            return Optional.of(Errors.from(finalErrors));
        }
        return Optional.empty();
    }

    void validateUpdateMerchantDetailsRequest(JsonNode payload) throws ValidationException {
        Optional<List<String>> errors = requestValidations.checkIfExistsOrEmpty(payload,
                FIELD_MERCHANT_DETAILS_NAME, FIELD_MERCHANT_DETAILS_ADDRESS_LINE1,
                FIELD_MERCHANT_DETAILS_ADDRESS_CITY, FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE,
                FIELD_MERCHANT_DETAILS_ADDRESS_COUNTRY);
        if (errors.isPresent()) {
            throw new ValidationException(Errors.from(errors.get()));
        }

        if (payload.has(FIELD_MERCHANT_DETAILS_EMAIL)) {
            validateMerchantEmail(payload);
        }
    }

    private void validateMerchantEmail(JsonNode payload) throws ValidationException {
        Optional<List<String>> errors;
        errors = requestValidations.checkMaxLength(payload, FIELD_MERCHANT_DETAILS_EMAIL_MAX_LENGTH,
                FIELD_MERCHANT_DETAILS_EMAIL);
        if (errors.isPresent()) {
            throw new ValidationException(Errors.from(errors.get()));
        }

        errors = requestValidations.isValidEmail(payload, FIELD_MERCHANT_DETAILS_EMAIL);
        if (errors.isPresent()) {
            throw new ValidationException(Errors.from(errors.get()));
        }
    }

    Optional<Errors> validateFindRequest(String gatewayAccountId) {
        if (isBlank(gatewayAccountId)) {
            return Optional.of(Errors.from("Find services currently support only by gatewayAccountId"));
        }
        return Optional.empty();
    }

    private static List<String> checkIfValidJson(JsonNode payload, String fieldName) {
        if (payload == null || !payload.isObject()) {
            return Collections.singletonList(format("Value for path [%s] must be a JSON", fieldName));
        }
        return Collections.emptyList();
    }

    private static List<String> validateOperations(JsonNode operations) {
        List<String> finalErrors = new ArrayList<>();
        operations.forEach(item -> {
            String path = item.get(FIELD_PATH).asText();

            if (!VALID_ATTRIBUTE_UPDATE_OPERATIONS.keySet().contains(path)) {
                finalErrors.add(format("Path [%s] is invalid", path));
                return;
            }

            String op = item.get("op").asText();
            if (!VALID_ATTRIBUTE_UPDATE_OPERATIONS.get(path).contains(op)) {
                finalErrors.add(format("Operation [%s] is invalid for path [%s]", op, path));
            }
        });

        return finalErrors;
    }

    private List<String> validatePath(JsonNode operations) {
        List<String> finalErrors = new ArrayList<>();
        operations.forEach(item -> {
            String path = item.get(FIELD_PATH).asText();

            if (FIELD_CUSTOM_BRANDING.equals(path)) {
                finalErrors.addAll(checkIfValidJson(item.get(FIELD_VALUE), FIELD_CUSTOM_BRANDING));
            } else if (FIELD_NAME.equals(path)) {
                requestValidations.checkIfExistsOrEmpty(item, FIELD_VALUE).ifPresent(finalErrors::addAll);
                if (finalErrors.isEmpty()) {
                    requestValidations.checkMaxLength(item, SERVICE_NAME_MAX_LENGTH, FIELD_VALUE).ifPresent(finalErrors::addAll);
                }
            }
        });

        return finalErrors;
    }

    private List<String> validateOpAndPathIfExistOrEmpty(JsonNode operations) {
        List<String> finalErrors = new ArrayList<>();
        operations.forEach(item ->
                requestValidations.checkIfExistsOrEmpty(item, FIELD_OP, FIELD_PATH).ifPresent(finalErrors::addAll));

        return finalErrors;
    }
}
