package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.utils.Errors;

import java.util.Optional;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.tuple.Pair.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserRequestValidatorTest {
    UserRequestValidator validator;

    @Before
    public void before() throws Exception {
        validator = new UserRequestValidator();
    }

    @Test
    public void shouldError_ifAllMandatoryFieldsAreMissing() throws Exception {
        JsonNode invalidPayload = mock(JsonNode.class);
        when(invalidPayload.get(anyString())).thenReturn(null);
        Optional<Errors> optionalErrors = validator.validateCreateRequest(invalidPayload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(5));
        assertThat(errors.getErrors(), hasItems(
                "Field [username] is required",
                "Field [email] is required",
                "Field [gatewayAccountId] is required",
                "Field [telephoneNumber] is required",
                "Field [roleName] is required"));

    }

    @Test
    public void shouldError_ifSomeMandatoryFieldsAreMissing() throws Exception {
        JsonNode invalidPayload = mock(JsonNode.class);
        mockValidValuesFor(invalidPayload,
                of("username", "blah"), of("email", "blah@blah.com"), of("otpKey", "blahblah"));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(invalidPayload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(3));
        assertThat(errors.getErrors(), hasItems(
                "Field [gatewayAccountId] is required",
                "Field [roleName] is required",
                "Field [telephoneNumber] is required"));

    }

    @Test
    public void shouldError_ifNumericFieldsAreNotNumeric() throws Exception {
        JsonNode invalidPayload = mock(JsonNode.class);
        mockValidValuesFor(invalidPayload,
                of("username", "blah"), of("password", "blah pass"), of("gatewayAccountId", "gatewayAccountId"),
                of("telephoneNumber", "telephoneNumber"), of("email", "blah@blah.com"), of("otpKey", "blahblah"),
                of("roleName","boo"));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(invalidPayload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(2));
        assertThat(errors.getErrors(), hasItems(
                "Field [gatewayAccountId] must be a number",
                "Field [telephoneNumber] must be a number"));

    }

    @Test
    public void shouldReturnEmpty_ifAllValidationsArePassed() throws Exception {
        JsonNode invalidPayload = mock(JsonNode.class);
        mockValidValuesFor(invalidPayload,
                of("username", "blah"), of("gatewayAccountId", "1"),
                of("telephoneNumber", "3534876538"), of("email", "blah@blah.com"),
                of("roleName","yah"));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(invalidPayload);

        assertFalse(optionalErrors.isPresent());
    }

    private void mockValidValuesFor(JsonNode mockJsonNode, Pair<String, String>... mockFieldValues) {
        for (Pair<String, String> mockFieldValue : mockFieldValues) {
            JsonNode fieldMock = mock(JsonNode.class);
            when(fieldMock.asText()).thenReturn(mockFieldValue.getRight());
            when(mockJsonNode.get(mockFieldValue.getLeft())).thenReturn(fieldMock);
        }
    }
}
