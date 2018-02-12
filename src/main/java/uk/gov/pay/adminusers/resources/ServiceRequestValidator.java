package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.math.NumberUtils;
import uk.gov.pay.adminusers.exception.ValidationException;
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
    public static final String FIELD_MERCHANT_DETAILS_NAME = "name";
    public static final String FIELD_MERCHANT_DETAILS_ADDRESS_LINE1 = "address_line1";
    public static final String FIELD_MERCHANT_DETAILS_ADDRESS_CITY = "address_city";
    public static final String FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE = "address_postcode";
    public static final String FIELD_MERCHANT_DETAILS_ADDRESS_COUNTRY = "address_country";

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

    public Optional<Errors> validateUpdateAttributeRequest(JsonNode payload) {
        Optional<List<String>> errors = requestValidations.checkIfExistsOrEmpty(payload, FIELD_OP, FIELD_PATH);
        if (errors.isPresent()) {
            return Optional.of(Errors.from(errors.get()));
        }

        String path = payload.get("path").asText();
        if (!FIELD_CUSTOM_BRANDING.equals(path)) {
            errors = requestValidations.checkIfExistsOrEmpty(payload, FIELD_VALUE);
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

    public void validateUpdateMerchantDetailsRequest(JsonNode payload) throws ValidationException {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkIfExistsOrEmpty(payload,
                FIELD_MERCHANT_DETAILS_NAME, FIELD_MERCHANT_DETAILS_ADDRESS_LINE1,
                FIELD_MERCHANT_DETAILS_ADDRESS_CITY, FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE,
                FIELD_MERCHANT_DETAILS_ADDRESS_COUNTRY);
        if (missingMandatoryFields.isPresent()) {
            throw new ValidationException(Errors.from(missingMandatoryFields.get()));
        }
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
