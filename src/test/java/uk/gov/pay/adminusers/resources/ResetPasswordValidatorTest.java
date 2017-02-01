package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.persistence.entity.ForgottenPasswordEntity;
import uk.gov.pay.adminusers.utils.Errors;

import java.util.Optional;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResetPasswordValidatorTest {

    private ResetPasswordValidator resetPasswordValidator;
    private ForgottenPasswordDao forgottenPasswordDao;

    @Before
    public void before() throws Exception {
        forgottenPasswordDao = mock(ForgottenPasswordDao.class);
        resetPasswordValidator = new ResetPasswordValidator();
    }

    @Test
    public void shouldReturnErrors_ifJsonNodeIsNull() throws Exception {

        Optional<Errors> errorsOptional = resetPasswordValidator.validateResetRequest(null);

        assertTrue(errorsOptional.isPresent());
        assertThat(errorsOptional.get().getErrors(), hasItem("JsonNode is invalid"));
    }

    @Test
    public void shouldReturnErrors_ifForgottenPasswordCodeIsBlank(){
        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode codeMock = mock(JsonNode.class);
        when(jsonNode.get("forgotten_password_code")).thenReturn(codeMock);
        when(codeMock.asText()).thenReturn("");
        Optional<Errors> errorsOptional = resetPasswordValidator.validateResetRequest(jsonNode);

        assertTrue(errorsOptional.isPresent());
        assertThat(errorsOptional.get().getErrors(), hasItem("Field [forgotten_password_code] is invalid"));
    }

    @Test
    public void shouldReturnErrors_ifNewPasswordIsBlank(){
        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode codeMock = mock(JsonNode.class);
        JsonNode passwordMock = mock(JsonNode.class);

        when(jsonNode.get("forgotten_password_code")).thenReturn(codeMock);
        when(codeMock.asText()).thenReturn("a-valid-code");
        when(jsonNode.get("new_password")).thenReturn(passwordMock);
        when(passwordMock.asText()).thenReturn("");

        Optional<Errors> errorsOptional = resetPasswordValidator.validateResetRequest(jsonNode);

        assertTrue(errorsOptional.isPresent());
        assertThat(errorsOptional.get().getErrors(), hasItem("Field [new_password] is invalid"));
    }

    @Test
    public void shouldReturnEmptyOptional(){
        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode codeMock = mock(JsonNode.class);
        JsonNode passwordMock = mock(JsonNode.class);
        when(jsonNode.get("forgotten_password_code")).thenReturn(codeMock);
        when(codeMock.asText()).thenReturn("a-valid-code");
        when(jsonNode.get("new_password")).thenReturn(passwordMock);
        when(passwordMock.asText()).thenReturn("password");

        when(forgottenPasswordDao.findNonExpiredByCode(jsonNode.asText())).thenReturn(Optional.empty());

        Optional<Errors> errorsOptional = resetPasswordValidator.validateResetRequest(jsonNode);

        assertFalse(errorsOptional.isPresent());
    }
}
