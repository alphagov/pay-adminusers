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

public class GovUkPayAgreementRequestValidatorTest {
    
    private GovUkPayAgreementRequestValidator validator;
    
    @Before
    public void setUp() {
        validator = new GovUkPayAgreementRequestValidator(new RequestValidations());
    }
    
    @Test
    public void shouldPassValidation() {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("user_external_id", "abcde1234"));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(payload);
        assertThat(optionalErrors.isPresent(), is(false));
    }
    
    @Test
    public void shouldFailValidation_whenUserExternalIdIsEmpty() {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("user_external_id", ""));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(payload);
        assertThat(optionalErrors.isPresent(), is(true));
        assertThat(optionalErrors.get().getErrors().size(), is(1));
        assertThat(optionalErrors.get().getErrors(), hasItems("Field [user_external_id] is required"));
    }
    
    @Test
    public void shouldFailValidation_whenUserExternalIdIsNotString() {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("user_external_id", true));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(payload);
        assertThat(optionalErrors.isPresent(), is(true));
        assertThat(optionalErrors.get().getErrors().size(), is(1));
        assertThat(optionalErrors.get().getErrors(), hasItems("Field [user_external_id] must be a valid user ID"));
    }
    
    @Test
    public void shouldFailValidation_whenUserExternalIdIsMissing() {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("luser_external_id", "abcde1234"));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(payload);
        assertThat(optionalErrors.isPresent(), is(true));
        assertThat(optionalErrors.get().getErrors().size(), is(1));
        assertThat(optionalErrors.get().getErrors(), hasItems("Field [user_external_id] is required"));
    }
}
