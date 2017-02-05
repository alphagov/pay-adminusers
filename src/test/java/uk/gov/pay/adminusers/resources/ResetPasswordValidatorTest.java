package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.Optional;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResetPasswordValidatorTest {

    private ResetPasswordValidator resetPasswordValidator;
    private ForgottenPasswordDao forgottenPasswordDao;

    @Before
    public void before() throws Exception {
        forgottenPasswordDao = mock(ForgottenPasswordDao.class);
        resetPasswordValidator = new ResetPasswordValidator(new RequestValidations());
    }

    @Test
    public void shouldReturnErrors_ifJsonNodeIsNull() throws Exception {

        Optional<Errors> errorsOptional = resetPasswordValidator.validateResetRequest(null);

        assertTrue(errorsOptional.isPresent());
        assertThat(errorsOptional.get().getErrors().size(), is(1));
        assertThat(errorsOptional.get().getErrors(), hasItem("invalid JSON"));
    }

    @Test
    public void shouldReturnErrors_ifForgottenPasswordCodeIsBlank() {

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode codeMock = mock(JsonNode.class);
        JsonNode passwordMock = mock(JsonNode.class);

        when(jsonNode.get("forgotten_password_code")).thenReturn(codeMock);
        when(jsonNode.get("new_password")).thenReturn(passwordMock);
        when(codeMock.asText()).thenReturn(" ");
        when(passwordMock.asText()).thenReturn("myNewPassword");

        Optional<Errors> errorsOptional = resetPasswordValidator.validateResetRequest(jsonNode);

        assertTrue(errorsOptional.isPresent());
        assertThat(errorsOptional.get().getErrors().size(), is(1));
        assertThat(errorsOptional.get().getErrors(), hasItem("Field [forgotten_password_code] is required"));
    }

    @Test
    public void shouldReturnErrors_ifForgottenPasswordCodeExceeds255Characters() {

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode codeMock = mock(JsonNode.class);
        JsonNode passwordMock = mock(JsonNode.class);

        when(jsonNode.get("forgotten_password_code")).thenReturn(codeMock);
        when(jsonNode.get("new_password")).thenReturn(passwordMock);
        when(codeMock.asText()).thenReturn(RandomStringUtils.randomAlphanumeric(256));
        when(passwordMock.asText()).thenReturn("myNewPassword");

        Optional<Errors> errorsOptional = resetPasswordValidator.validateResetRequest(jsonNode);

        assertTrue(errorsOptional.isPresent());
        assertThat(errorsOptional.get().getErrors().size(), is(1));
        assertThat(errorsOptional.get().getErrors(), hasItem("Field [forgotten_password_code] must have a maximum length of 255 characters"));
    }

    @Test
    public void shouldReturnErrors_ifNewPasswordIsBlank() {

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode codeMock = mock(JsonNode.class);
        JsonNode passwordMock = mock(JsonNode.class);

        when(jsonNode.get("forgotten_password_code")).thenReturn(codeMock);
        when(jsonNode.get("new_password")).thenReturn(passwordMock);
        when(codeMock.asText()).thenReturn("myCode");
        when(passwordMock.asText()).thenReturn(" ");

        Optional<Errors> errorsOptional = resetPasswordValidator.validateResetRequest(jsonNode);

        assertTrue(errorsOptional.isPresent());
        assertThat(errorsOptional.get().getErrors().size(), is(1));
        assertThat(errorsOptional.get().getErrors(), hasItem("Field [new_password] is required"));
    }

    @Test
    public void shouldReturnEmptyOptional() {
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
