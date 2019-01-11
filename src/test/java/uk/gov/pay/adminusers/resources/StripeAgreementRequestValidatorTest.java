package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;

public class StripeAgreementRequestValidatorTest {

    private StripeAgreementRequestValidator validator;

    @Before
    public void before() {
        validator = new StripeAgreementRequestValidator(new RequestValidations());
    }
    
    @Test
    public void shouldPassValidIPv4Address() {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("ip_address", "192.0.2.0"));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(payload);
        assertThat(optionalErrors.isPresent(), is(false));
    }

    @Test
    public void shouldPassValidIPv6Address() {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("ip_address", "2001:DB8:0000:0000:0000:0000:0000:0000"));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(payload);
        assertThat(optionalErrors.isPresent(), is(false));
    }

    @Test
    public void shouldErrorForInvalidIPAddress() {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("ip_address", "257.0.2.0"));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(payload);
        assertThat(optionalErrors.isPresent(), is(true));
        assertThat(optionalErrors.get().getErrors().size(), is(1));
        assertThat(optionalErrors.get().getErrors(), hasItems("Field [ip_address] must be a valid IP address"));
    }

    @Test
    public void shouldErrorWhenIpAddressIsNotAString() {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("ip_address", 1234));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(payload);
        assertThat(optionalErrors.isPresent(), is(true));
        assertThat(optionalErrors.get().getErrors().size(), is(1));
        assertThat(optionalErrors.get().getErrors(), hasItems("Field [ip_address] must be a valid IP address"));
    }

    @Test
    public void shouldErrorWhenIpAddressIsEmpty() {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("ip_address", ""));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(payload);
        assertThat(optionalErrors.isPresent(), is(true));
        assertThat(optionalErrors.get().getErrors().size(), is(1));
        assertThat(optionalErrors.get().getErrors(), hasItems("Field [ip_address] is required"));
    }

    @Test
    public void shouldErrorWhenIpAddressIsMissing() {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("not_ip_address", ""));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(payload);
        assertThat(optionalErrors.isPresent(), is(true));
        assertThat(optionalErrors.get().getErrors().size(), is(1));
        assertThat(optionalErrors.get().getErrors(), hasItems("Field [ip_address] is required"));
    }


}
