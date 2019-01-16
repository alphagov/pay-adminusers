package uk.gov.pay.adminusers.resources;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Documented
@Constraint(validatedBy = IpAddressValidator.class)
public @interface ValidIpAddress {
    
    String message() default "must be valid IP address";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default{};
}
