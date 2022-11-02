package uk.gov.pay.adminusers.validations;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    private static final org.apache.commons.validator.routines.EmailValidator COMMONS_EMAIL_VALIDATOR =
            org.apache.commons.validator.routines.EmailValidator.getInstance();
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        return value == null || COMMONS_EMAIL_VALIDATOR.isValid(value);
    }
}
