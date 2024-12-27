package uk.gov.pay.adminusers.app.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {SqsConfigValidator.class})
@Documented
public @interface ValidSqsConfig {
    String message() default "{uk.gov.pay.adminusers.app.validator.sqsConfig.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
