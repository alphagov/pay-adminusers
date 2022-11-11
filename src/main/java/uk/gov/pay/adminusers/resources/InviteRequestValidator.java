package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.utils.telephonenumber.TelephoneNumberUtility;
import uk.gov.pay.adminusers.validations.RequestValidations;
import uk.gov.service.payments.commons.api.exception.ValidationException;
import uk.gov.service.payments.commons.api.validation.JsonPatchRequestValidator;
import uk.gov.service.payments.commons.api.validation.PatchPathOperation;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchOp;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static java.lang.CharSequence.compare;
import static java.lang.String.format;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_CODE;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_PASSWORD;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_TELEPHONE_NUMBER;
import static uk.gov.service.payments.commons.api.validation.JsonPatchRequestValidator.throwIfValueNotString;

public class InviteRequestValidator {

    private static final int MAX_LENGTH_CODE = 255;
    private final RequestValidations requestValidations;

    public static final String FIELD_PASSWORD = "password";
    
    public static final String FIELD_TELEPHONE_NUMBER = "telephone_number";
    
    private static final Map<PatchPathOperation, Consumer<JsonPatchRequest>> patchOperationValidators = Map.of(
            new PatchPathOperation(FIELD_PASSWORD, JsonPatchOp.REPLACE), JsonPatchRequestValidator::throwIfValueNotString,
            new PatchPathOperation(FIELD_TELEPHONE_NUMBER, JsonPatchOp.REPLACE), InviteRequestValidator::validateReplaceTelephoneNumberOperation
    );
    private final JsonPatchRequestValidator patchRequestValidator = new JsonPatchRequestValidator(patchOperationValidators);

    @Inject
    public InviteRequestValidator(RequestValidations requestValidations) {
        this.requestValidations = requestValidations;
    }

    public Optional<Errors> validateGenerateOtpRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkExistsAndNotEmpty(payload, FIELD_TELEPHONE_NUMBER, FIELD_PASSWORD);
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }
        if (payload.get(FIELD_CODE) != null) {
            Optional<List<String>> invalidLength = requestValidations.checkMaxLength(payload, MAX_LENGTH_CODE, FIELD_CODE);
            return invalidLength.map(Errors::from);
        }
        return Optional.empty();
    }


    public Optional<Errors> validateResendOtpRequest(JsonNode payload) {
        Optional<List<String>> missingMandatoryFields = requestValidations.checkExistsAndNotEmpty(payload, FIELD_CODE, FIELD_TELEPHONE_NUMBER);
        if (missingMandatoryFields.isPresent()) {
            return Optional.of(Errors.from(missingMandatoryFields.get()));
        }
        Optional<List<String>> invalidLength = requestValidations.checkMaxLength(payload, MAX_LENGTH_CODE, FIELD_CODE);
        return invalidLength.map(Errors::from);
    }
    
    public void validatePatchRequest(JsonNode patchRequest) {
        patchRequestValidator.validate(patchRequest);
    }
    
    private static void validateReplaceTelephoneNumberOperation(JsonPatchRequest request) {
        throwIfValueNotString(request);
        if (!TelephoneNumberUtility.isValidPhoneNumber(request.valueAsString())) {
            throw new ValidationException(Collections.singletonList(format("Value for path [%s] must be a valid telephone number", FIELD_TELEPHONE_NUMBER)));
        }
    }
}
