package uk.gov.pay.adminusers.validations;

import uk.gov.pay.adminusers.utils.telephonenumber.TelephoneNumberUtility;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TelephoneNumberValidator implements ConstraintValidator<ValidTelephoneNumber, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        return value == null || TelephoneNumberUtility.isValidPhoneNumber(value);
    }
}
