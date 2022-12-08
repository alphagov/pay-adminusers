package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.pay.adminusers.utils.telephonenumber.TelephoneNumberUtility;
import uk.gov.service.payments.commons.api.exception.ValidationException;
import uk.gov.service.payments.commons.api.validation.JsonPatchRequestValidator;
import uk.gov.service.payments.commons.api.validation.PatchPathOperation;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchOp;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchRequest;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.String.format;
import static uk.gov.service.payments.commons.api.validation.JsonPatchRequestValidator.throwIfValueNotString;

public class InviteRequestValidator {

    public static final String FIELD_PASSWORD = "password";
    
    public static final String FIELD_TELEPHONE_NUMBER = "telephone_number";
    
    private static final Map<PatchPathOperation, Consumer<JsonPatchRequest>> patchOperationValidators = Map.of(
            new PatchPathOperation(FIELD_PASSWORD, JsonPatchOp.REPLACE), JsonPatchRequestValidator::throwIfValueNotString,
            new PatchPathOperation(FIELD_TELEPHONE_NUMBER, JsonPatchOp.REPLACE), InviteRequestValidator::validateReplaceTelephoneNumberOperation
    );
    private final JsonPatchRequestValidator patchRequestValidator = new JsonPatchRequestValidator(patchOperationValidators);
    
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
