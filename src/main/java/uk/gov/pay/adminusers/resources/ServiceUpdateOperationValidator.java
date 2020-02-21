package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.pay.adminusers.model.GoLiveStage;
import uk.gov.pay.adminusers.validations.RequestValidations;
import uk.gov.pay.commons.model.SupportedLanguage;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Map.entry;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_OP;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_PATH;
import static uk.gov.pay.adminusers.model.ServiceUpdateRequest.FIELD_VALUE;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_COLLECT_BILLING_ADDRESS;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_CURRENT_GO_LIVE_STAGE;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_CUSTOM_BRANDING;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_GATEWAY_ACCOUNT_IDS;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_MERCHANT_DETAILS_ADDRESS_CITY;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_MERCHANT_DETAILS_ADDRESS_COUNRTY;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_MERCHANT_DETAILS_ADDRESS_LINE_1;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_MERCHANT_DETAILS_ADDRESS_LINE_2;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_MERCHANT_DETAILS_EMAIL;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_MERCHANT_DETAILS_NAME;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_MERCHANT_DETAILS_TELEPHONE_NUMBER;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_REDIRECT_NAME;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_SERVICE_NAME_PREFIX;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_EXPERIMENTAL_FEATURES_ENABLED;

public class ServiceUpdateOperationValidator {

    private static final String REPLACE = "replace";
    private static final String ADD = "add";

    private static final int SERVICE_NAME_MAX_LENGTH = 50;

    private static final int FIELD_MERCHANT_DETAILS_NAME_MAX_LENGTH = 255;
    private static final int FIELD_MERCHANT_DETAILS_ADDRESS_LINE_1_MAX_LENGTH = 255;
    private static final int FIELD_MERCHANT_DETAILS_ADDRESS_LINE_2_MAX_LENGTH = 255;
    private static final int FIELD_MERCHANT_DETAILS_ADDRESS_CITY_MAX_LENGTH = 255;
    private static final int FIELD_MERCHANT_DETAILS_ADDRESS_COUNTRY_CODE_MAX_LENGTH = 10;
    private static final int FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE_MAX_LENGTH = 25;
    private static final int FIELD_MERCHANT_DETAILS_EMAIL_MAX_LENGTH = 255;
    private static final int FIELD_MERCHANT_DETAILS_TELEPHONE_NUMBER_MAX_LENGTH = 255;

    private final Map<String, List<String>> validAttributeUpdateOperations;

    private final RequestValidations requestValidations;

    private static final EnumSet<GoLiveStage> GO_LIVE_STAGES = EnumSet.allOf(GoLiveStage.class);

    @Inject
    public ServiceUpdateOperationValidator(RequestValidations requestValidations) {
        Map<String, List<String>> validAttributeUpdateOperations = new HashMap<>(Map.ofEntries(
                entry(FIELD_GATEWAY_ACCOUNT_IDS, singletonList(ADD)),
                entry(FIELD_CUSTOM_BRANDING, singletonList(REPLACE)),
                entry(FIELD_REDIRECT_NAME, singletonList(REPLACE)),
                entry(FIELD_EXPERIMENTAL_FEATURES_ENABLED, singletonList(REPLACE)),
                entry(FIELD_COLLECT_BILLING_ADDRESS, singletonList(REPLACE)),
                entry(FIELD_CURRENT_GO_LIVE_STAGE, singletonList(REPLACE)),
                entry(FIELD_MERCHANT_DETAILS_NAME, singletonList(REPLACE)),
                entry(FIELD_MERCHANT_DETAILS_ADDRESS_LINE_1, singletonList(REPLACE)),
                entry(FIELD_MERCHANT_DETAILS_ADDRESS_LINE_2, singletonList(REPLACE)),
                entry(FIELD_MERCHANT_DETAILS_ADDRESS_CITY, singletonList(REPLACE)),
                entry(FIELD_MERCHANT_DETAILS_ADDRESS_COUNRTY, singletonList(REPLACE)),
                entry(FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE, singletonList(REPLACE)),
                entry(FIELD_MERCHANT_DETAILS_EMAIL, singletonList(REPLACE)),
                entry(FIELD_MERCHANT_DETAILS_TELEPHONE_NUMBER, singletonList(REPLACE))
        ));
        Arrays.stream(SupportedLanguage.values()).forEach(lang ->
                validAttributeUpdateOperations.put(FIELD_SERVICE_NAME_PREFIX + '/' + lang.toString(), singletonList(REPLACE)));
        this.validAttributeUpdateOperations = Map.copyOf(validAttributeUpdateOperations);
        this.requestValidations = requestValidations;
    }

    /* default */ List<String> validate(JsonNode operation) {
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
        requestValidations.checkExistsAndNotEmpty(operation, FIELD_OP, FIELD_PATH).ifPresent(errors::addAll);
        return errors;
    }

    private List<String> validateValueIsValidForPath(JsonNode operation) {
        String path = operation.get(FIELD_PATH).asText();
        if (FIELD_CUSTOM_BRANDING.equals(path)) {
            return validateCustomBrandingValue(operation);
        } else if (path.startsWith(FIELD_SERVICE_NAME_PREFIX)) {
            return validateServiceNameValue(operation, path);
        } else if (FIELD_REDIRECT_NAME.equals(path)) {
            return validateMandatoryBooleanValue(operation);
        } else if (FIELD_EXPERIMENTAL_FEATURES_ENABLED.equals(path)) {
            return validateMandatoryBooleanValue(operation);
        } else if (FIELD_COLLECT_BILLING_ADDRESS.equals(path)) {
            return validateMandatoryBooleanValue(operation);
        } else if (FIELD_CURRENT_GO_LIVE_STAGE.equals(path)) {
            return validateCurrentGoLiveStageValue(operation);
        } else if (FIELD_MERCHANT_DETAILS_NAME.equals(path)) {
            return validateNotNullStringValueWithMaxLength(operation, false, FIELD_MERCHANT_DETAILS_NAME_MAX_LENGTH);
        } else if (FIELD_MERCHANT_DETAILS_ADDRESS_LINE_1.equals(path)) {
            return validateNotNullStringValueWithMaxLength(operation, false, FIELD_MERCHANT_DETAILS_ADDRESS_LINE_1_MAX_LENGTH);
        } else if (FIELD_MERCHANT_DETAILS_ADDRESS_LINE_2.equals(path)) {
            return validateNotNullStringValueWithMaxLength(operation, true, FIELD_MERCHANT_DETAILS_ADDRESS_LINE_2_MAX_LENGTH);
        } else if (FIELD_MERCHANT_DETAILS_ADDRESS_CITY.equals(path)) {
            return validateNotNullStringValueWithMaxLength(operation, false, FIELD_MERCHANT_DETAILS_ADDRESS_CITY_MAX_LENGTH);
        } else if (FIELD_MERCHANT_DETAILS_ADDRESS_COUNRTY.equals(path)) {
            return validateNotNullStringValueWithMaxLength(operation, false, FIELD_MERCHANT_DETAILS_ADDRESS_COUNTRY_CODE_MAX_LENGTH);
        } else if (FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE.equals(path)) {
            return validateNotNullStringValueWithMaxLength(operation, false, FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE_MAX_LENGTH);
        } else if (FIELD_MERCHANT_DETAILS_EMAIL.equals(path)) {
            return validateNotNullStringValueWithMaxLength(operation, true, FIELD_MERCHANT_DETAILS_EMAIL_MAX_LENGTH);
        } else if (FIELD_MERCHANT_DETAILS_TELEPHONE_NUMBER.equals(path)) {
            return validateNotNullStringValueWithMaxLength(operation, true, FIELD_MERCHANT_DETAILS_TELEPHONE_NUMBER_MAX_LENGTH);
        }

        return Collections.emptyList();
    }

    private List<String> validateCustomBrandingValue(JsonNode operation) {
        return checkIfValidJson(operation.get(FIELD_VALUE), FIELD_CUSTOM_BRANDING);
    }

    private List<String> validateServiceNameValue(JsonNode operation, String path) {
        boolean allowEmpty = !path.endsWith('/' + SupportedLanguage.ENGLISH.toString());
        return validateNotNullStringValueWithMaxLength(operation, allowEmpty, SERVICE_NAME_MAX_LENGTH);
    }

    private List<String> validateMandatoryBooleanValue(JsonNode operation) {
        List<String> errors = new ArrayList<>();
        requestValidations.checkExists(operation, FIELD_VALUE).ifPresent(errors::addAll);
        if (errors.isEmpty()) {
            requestValidations.checkIsStrictBoolean(operation, FIELD_VALUE).ifPresent(errors::addAll);
        }
        return errors;
    }

    private List<String> validateCurrentGoLiveStageValue(JsonNode operation) {
        List<String> errors = new ArrayList<>();
        requestValidations.checkExistsAndNotEmpty(operation, FIELD_VALUE).ifPresent(errors::addAll);
        if (errors.isEmpty()) {
            requestValidations.checkIsString(
                    format("Field [%s] must be one of %s", FIELD_VALUE, GO_LIVE_STAGES),
                    operation,
                    FIELD_VALUE).ifPresent(errors::addAll);
        }
        if (errors.isEmpty()) {
            requestValidations.isValidEnumValue(operation, GO_LIVE_STAGES, FIELD_VALUE).ifPresent(errors::addAll);
        }
        return errors;
    }

    private List<String> validateNotNullStringValueWithMaxLength(JsonNode operation, boolean allowEmpty, int maxLength) {
        List<String> errors = new ArrayList<>();
        if (allowEmpty) {
            requestValidations.checkExists(operation, FIELD_VALUE).ifPresent(errors::addAll);
        } else {
            requestValidations.checkExistsAndNotEmpty(operation, FIELD_VALUE).ifPresent(errors::addAll);
        }
        
        if (errors.isEmpty()) {
            requestValidations.checkIsString(operation, FIELD_VALUE).ifPresent(errors::addAll);
        }
        if (errors.isEmpty()) {
            requestValidations.checkMaxLength(operation, maxLength, FIELD_VALUE).ifPresent(errors::addAll);
        }
        
        return errors;
    }

    private List<String> validateOperationIsValidForPath(JsonNode operation) {
        String path = operation.get(FIELD_PATH).asText();

        if (!validAttributeUpdateOperations.containsKey(path)) {
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
