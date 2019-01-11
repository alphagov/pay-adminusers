package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.utils.Errors;

import java.util.Optional;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ForgottenPasswordValidatorTest {

    private ForgottenPasswordValidator validator;

    @Before
    public void before() {
        validator = new ForgottenPasswordValidator();
    }

    @Test
    public void shouldReturnErrors_ifJsonNodeIsNull() {

        Optional<Errors> errorsOptional = validator.validateCreateRequest(null);

        assertTrue(errorsOptional.isPresent());
        assertThat(errorsOptional.get().getErrors(), hasItem("Field [username] is required"));
    }

    @Test
    public void shouldReturnErrors_ifNoUserNameElement() {

        JsonNode jsonNode = mock(JsonNode.class);
        when(jsonNode.get("username")).thenReturn(null);
        Optional<Errors> errorsOptional = validator.validateCreateRequest(jsonNode);

        assertTrue(errorsOptional.isPresent());
        assertThat(errorsOptional.get().getErrors(), hasItem("Field [username] is required"));
    }

    @Test
    public void shouldReturnErrors_ifUsernameIsBlank() {

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode userNameMock = mock(JsonNode.class);
        when(jsonNode.get("username")).thenReturn(userNameMock);
        when(userNameMock.asText()).thenReturn(" ");
        Optional<Errors> errorsOptional = validator.validateCreateRequest(jsonNode);

        assertTrue(errorsOptional.isPresent());
        assertThat(errorsOptional.get().getErrors(), hasItem("Field [username] is required"));
    }

    @Test
    public void shouldReturnEmpty_ifAUsernameIsPresent() {
        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode userNameMock = mock(JsonNode.class);
        when(jsonNode.get("username")).thenReturn(userNameMock);
        when(userNameMock.asText()).thenReturn("a-user-name");
        Optional<Errors> errorsOptional = validator.validateCreateRequest(jsonNode);

        assertFalse(errorsOptional.isPresent());
    }
}
