package uk.gov.pay.adminusers.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.StripeAgreementRequest;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class StripeAgreementRequestTest {
    
    private Validator validator;
    
    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Test
    public void shouldPassValidIPv4Address() {
        StripeAgreementRequest stripeAgreementRequest = new StripeAgreementRequest("192.0.2.0");
        assertThat(validator.validate(stripeAgreementRequest).isEmpty(), is(true));
    }

    @Test
    public void shouldPassValidIPv6Address() {
        StripeAgreementRequest stripeAgreementRequest = new StripeAgreementRequest("2001:DB8:0000:0000:0000:0000:0000:0000");
        assertThat(validator.validate(stripeAgreementRequest).isEmpty(), is(true));
    }

    @Test
    public void shouldErrorForInvalidIPAddress() {
        StripeAgreementRequest stripeAgreementRequest = new StripeAgreementRequest("257.0.2.0");
        Set<ConstraintViolation<StripeAgreementRequest>> violations = validator.validate(stripeAgreementRequest);
        assertThat(violations.size(), is(1));
        assertThat(violations.iterator().next().getMessage(), is("must be valid IP address"));
    }

    @Test
    public void shouldErrorWhenIpAddressIsEmpty() {
        StripeAgreementRequest stripeAgreementRequest = new StripeAgreementRequest("");
        Set<ConstraintViolation<StripeAgreementRequest>> violations = validator.validate(stripeAgreementRequest);
        assertThat(violations.size(), is(1));
        assertThat(violations.iterator().next().getMessage(), is("must be valid IP address"));
    }
}
