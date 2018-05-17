package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.pay.adminusers.exception.ValidationException;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_OP;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_PATH;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_VALUE;


public class ServiceRequestValidator {

    public static final String FIELD_SERVICE_NAME = "name";
    public static final String FIELD_GATEWAY_ACCOUNT_IDS = "gateway_account_ids";
    public static final String FIELD_CUSTOM_BRANDING = "custom_branding";
    public static final String FIELD_MERCHANT_DETAILS_NAME = "name";
    public static final String FIELD_MERCHANT_DETAILS_ADDRESS_LINE1 = "address_line1";
    public static final String FIELD_MERCHANT_DETAILS_ADDRESS_CITY = "address_city";
    public static final String FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE = "address_postcode";
    public static final String FIELD_MERCHANT_DETAILS_ADDRESS_COUNTRY = "address_country";
    public static final String FIELD_MERCHANT_DETAILS_EMAIL = "email";
    private static final int SERVICE_NAME_MAX_LENGTH = 50;
    private static final int FIELD_MERCHANT_DETAILS_EMAIL_MAX_LENGTH = 255;
    private static final Map<String, List<String>> VALID_ATTRIBUTE_UPDATE_OPERATIONS = new HashMap<String, List<String>>() {{
        put(FIELD_SERVICE_NAME, asList("replace"));
        put(FIELD_GATEWAY_ACCOUNT_IDS, asList("add"));
        put(FIELD_CUSTOM_BRANDING, asList("replace"));
    }};

    private final RequestValidations requestValidations;

    @Inject
    public ServiceRequestValidator(RequestValidations requestValidations) {
        this.requestValidations = requestValidations;
    }

    public Optional<Errors> validateUpdateAttributeRequest(JsonNode payload) {
        Optional<List<String>> errors = requestValidations.checkIfExistsOrEmpty(payload, FIELD_OP, FIELD_PATH);
        if (errors.isPresent()) {
            return Optional.of(Errors.from(errors.get()));
        }

        String path = payload.get("path").asText();

        if (FIELD_CUSTOM_BRANDING.equals(path)) {
            errors = checkIfNotEmptyAndJson(payload.get(FIELD_VALUE));
        } else if (FIELD_SERVICE_NAME.equals(path)) {
            errors = requestValidations.checkIfExistsOrEmpty(payload, FIELD_VALUE);
            if (!errors.isPresent()) {
                errors = requestValidations.checkMaxLength(payload, SERVICE_NAME_MAX_LENGTH, FIELD_VALUE);
            }
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

    public void validateUpdateMerchantDetailsRequest(JsonNode payload) throws ValidationException {
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

    public Optional<Errors> validateFindRequest(String gatewayAccountId) {
        if (isBlank(gatewayAccountId)) {
            return Optional.of(Errors.from("Find services currently support only by gatewayAccountId"));
        }
        return Optional.empty();
    }

}
