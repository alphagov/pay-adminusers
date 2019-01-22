package uk.gov.pay.adminusers.resources;

import org.apache.commons.validator.routines.InetAddressValidator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class IpAddressValidator implements ConstraintValidator<ValidIpAddress, Object> {

    @Override
    public void initialize(ValidIpAddress constraintAnnotation) {
        
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // it is considered best practice to return true if value is null and use the @NotNull constraint where
        // necessary
        return value == null || InetAddressValidator.getInstance().isValid(value.toString());
    }
}
