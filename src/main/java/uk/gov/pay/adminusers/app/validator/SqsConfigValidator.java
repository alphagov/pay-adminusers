package uk.gov.pay.adminusers.app.validator;

import uk.gov.pay.adminusers.app.config.SqsConfig;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class SqsConfigValidator
        implements ConstraintValidator<ValidSqsConfig, SqsConfig> {

    @Override
    public boolean isValid(SqsConfig sqsConfig, ConstraintValidatorContext constraintValidatorContext) {

        if (sqsConfig.isNonStandardServiceEndpoint()) {
            boolean isInvalidEndpointConfig = isEmpty(sqsConfig.getEndpoint())
                    || isEmpty(sqsConfig.getSecretKey())
                    || isEmpty(sqsConfig.getAccessKey());

            if (isInvalidEndpointConfig) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate("[endpoint, secretKey, accessKey] fields must be set, when `nonStandardServiceEndpoint` is true")
                        .addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}
