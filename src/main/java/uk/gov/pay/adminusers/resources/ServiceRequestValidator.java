package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.pay.adminusers.exception.ValidationException;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class ServiceRequestValidator {
    
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
    private static final int FIELD_MERCHANT_DETAILS_EMAIL_MAX_LENGTH = 255;

    private final RequestValidations requestValidations;
    private final ServiceUpdateOperationValidator serviceUpdateOperationValidator;

    @Inject
    public ServiceRequestValidator(RequestValidations requestValidations, ServiceUpdateOperationValidator serviceUpdateOperationValidator) {
        this.requestValidations = requestValidations;
        this.serviceUpdateOperationValidator = serviceUpdateOperationValidator;
    }

    Optional<Errors> validateUpdateAttributeRequest(JsonNode payload) {
        List<String> errors = new ArrayList<>();
        if (!payload.isArray()) {
            errors  = serviceUpdateOperationValidator.validate(payload);
        } else {
            for (JsonNode updateOperation : payload) {
                errors.addAll(serviceUpdateOperationValidator.validate(updateOperation));
            }
        }
        if (!errors.isEmpty()) {
            return Optional.of(Errors.from(errors));
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

}
