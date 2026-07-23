package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.pay.adminusers.model.ServiceSearchRequest;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;
import uk.gov.service.payments.commons.api.exception.ValidationException;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class ServiceRequestValidator {

    /* default */ static final String FIELD_MERCHANT_DETAILS_NAME = "name";
    /* default */ static final String FIELD_MERCHANT_DETAILS_ADDRESS_LINE1 = "address_line1";
    /* default */ static final String FIELD_MERCHANT_DETAILS_ADDRESS_CITY = "address_city";
    /* default */ static final String FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE = "address_postcode";
    /* default */ static final String FIELD_MERCHANT_DETAILS_ADDRESS_COUNTRY = "address_country";
    /* default */ static final String FIELD_MERCHANT_DETAILS_EMAIL = "email";

    public static final String SERVICE_SEARCH_SUPPORT_ERR_MSG = "Search only supports searching by service name or merchant name";
    public static final String SERVICE_SEARCH_LENGTH_ERR_MSG = "Search strings can only be 60 characters or less";
    public static final String SERVICE_SEARCH_SPECIAL_CHARS_ERR_MSG = "Search strings can only contain letters, numbers, spaces, apostrophes and hyphens";

    private static final int FIELD_MERCHANT_DETAILS_NAME_MAX_LENGTH = 255;
    private static final int FIELD_MERCHANT_DETAILS_EMAIL_MAX_LENGTH = 255;
    private static final int MAX_SEARCH_STRING_LENGTH = 60;

    private static final Pattern ALLOWED_SEARCH_CHARS = Pattern.compile("^[0-9A-Za-z'’\\-\\s]+$");

    private final RequestValidations requestValidations;
    private final ServiceUpdateOperationValidator serviceUpdateOperationValidator;

    @Inject
    public ServiceRequestValidator(RequestValidations requestValidations, ServiceUpdateOperationValidator serviceUpdateOperationValidator) {
        this.requestValidations = requestValidations;
        this.serviceUpdateOperationValidator = serviceUpdateOperationValidator;
    }

    /* default */ Optional<Errors> validateUpdateAttributeRequest(JsonNode payload) {
        List<String> errors = new ArrayList<>();

        if (payload.isArray()) {
            for (JsonNode updateOperation : payload) {
                errors.addAll(serviceUpdateOperationValidator.validate(updateOperation));
            }
        } else {
            errors = serviceUpdateOperationValidator.validate(payload);
        }

        if (!errors.isEmpty()) {
            return Optional.of(Errors.from(errors));
        }

        return Optional.empty();
    }

    /* default */ void validateUpdateMerchantDetailsRequest(JsonNode payload) throws ValidationException {
        Optional<List<String>> missingMandatoryFieldErrors = requestValidations.checkExistsAndNotEmpty(payload,
                FIELD_MERCHANT_DETAILS_NAME, FIELD_MERCHANT_DETAILS_ADDRESS_LINE1,
                FIELD_MERCHANT_DETAILS_ADDRESS_CITY, FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE,
                FIELD_MERCHANT_DETAILS_ADDRESS_COUNTRY);
        if (missingMandatoryFieldErrors.isPresent()) {
            throw new ValidationException(missingMandatoryFieldErrors.get());
        }

        Optional<List<String>> invalidLengthFieldErrors = requestValidations.checkMaxLength(payload, FIELD_MERCHANT_DETAILS_NAME_MAX_LENGTH, FIELD_MERCHANT_DETAILS_NAME);
        if (invalidLengthFieldErrors.isPresent()) {
            throw new ValidationException(invalidLengthFieldErrors.get());
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
            throw new ValidationException(errors.get());
        }

        errors = requestValidations.isValidEmail(payload, FIELD_MERCHANT_DETAILS_EMAIL);
        if (errors.isPresent()) {
            throw new ValidationException(errors.get());
        }
    }

    /* default */ Optional<Errors> validateFindRequest(String gatewayAccountId) {
        if (isBlank(gatewayAccountId)) {
            return Optional.of(Errors.from("Find services currently support only by gatewayAccountId"));
        }
        return Optional.empty();
    }

    public Optional<Errors> validateSearchRequest(ServiceSearchRequest request) {
        var errorList = new ArrayList<String>();
        var values = request.toMap().values().stream()
                .filter(value -> !isBlank(value))
                .collect(Collectors.toList());

        if (values.isEmpty()) {
            errorList.add(SERVICE_SEARCH_SUPPORT_ERR_MSG);
        } else {
            values.forEach(value -> {
                if (lengthValidator(value)) {
                    errorList.add(SERVICE_SEARCH_LENGTH_ERR_MSG);
                }
                if (!ALLOWED_SEARCH_CHARS.matcher(value).matches()) {
                    errorList.add(SERVICE_SEARCH_SPECIAL_CHARS_ERR_MSG);
                }
            });
        }

        return errorList.size() > 0 ? Optional.of(Errors.from(errorList)) : Optional.empty();
    }

    private boolean lengthValidator(String checkValue) {
        return (!isBlank(checkValue) && checkValue.length() > MAX_SEARCH_STRING_LENGTH);
    }

}
