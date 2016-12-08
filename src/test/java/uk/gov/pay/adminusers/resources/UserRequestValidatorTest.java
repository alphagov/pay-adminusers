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
                "Field [gateway_account_id] is required",
                "Field [telephone_number] is required",
                "Field [role_name] is required"));

    }

    @Test
    public void shouldError_ifSomeMandatoryFieldsAreMissing() throws Exception {
        JsonNode invalidPayload = mock(JsonNode.class);
        mockValidValuesFor(invalidPayload,
                of("username", "blah"), of("email", "blah@blah.com"), of("otp_key", "blahblah"));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(invalidPayload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(3));
        assertThat(errors.getErrors(), hasItems(
                "Field [gateway_account_id] is required",
                "Field [role_name] is required",
                "Field [telephone_number] is required"));

    }

    @Test
    public void shouldError_ifNumericFieldsAreNotNumeric() throws Exception {
        JsonNode invalidPayload = mock(JsonNode.class);
        mockValidValuesFor(invalidPayload,
                of("username", "blah"), of("password", "blah pass"), of("gateway_account_id", "gatewayAccountId"),
                of("telephone_number", "telephoneNumber"), of("email", "blah@blah.com"), of("otp_key", "blahblah"),
                of("role_name","boo"));
        Optional<Errors> optionalErrors = validator.validateCreateRequest(invalidPayload);

        assertTrue(optionalErrors.isPresent());
        Errors errors = optionalErrors.get();

        assertThat(errors.getErrors().size(), is(2));
        assertThat(errors.getErrors(), hasItems(
                "Field [gateway_account_id] must be a number",
                "Field [telephone_number] must be a number"));

    }

    @Test
    public void shouldReturnEmpty_ifAllValidationsArePassed() throws Exception {
        JsonNode invalidPayload = mock(JsonNode.class);
        mockValidValuesFor(invalidPayload,
                of("username", "blah"), of("gateway_account_id", "1"),
                of("telephone_number", "3534876538"), of("email", "blah@blah.com"),
                of("role_name","yah"));
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
