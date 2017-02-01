package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ForgottenPasswordEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.utils.Errors;

import java.time.ZonedDateTime;
import java.util.Optional;

import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResetPasswordServiceTest {

    ResetPasswordService resetPasswordService;
    ForgottenPasswordDao forgottenPasswordDao;
    PasswordHasher passwordHasher;
    UserDao userDao;
    JsonNode jsonNode;
    JsonNode codeMock;
    JsonNode passwordMock;

    @Before
    public void before() throws Exception {
        userDao = mock(UserDao.class);
        forgottenPasswordDao = mock(ForgottenPasswordDao.class);
        passwordHasher = mock(PasswordHasher.class);
        setUpJsonNodes();
        resetPasswordService = new ResetPasswordService(userDao, forgottenPasswordDao, passwordHasher);
    }

    @Test
    public void shouldReturnErrors_whenCodeExpired(){

        String code = "forgotten_password_code";
        when(forgottenPasswordDao.findNonExpiredByCode(code)).thenReturn(Optional.empty());

        Optional<Errors> errorsOptional = resetPasswordService.updatePassword(jsonNode);

        assertTrue(errorsOptional.isPresent());
        assertThat(errorsOptional.get().getErrors(), hasItem("Field [forgotten_password_code] has expired"));
    }

    @Test
    public void shouldReturnEmptyOptional_whenCodeValid(){
        setUpJsonNodes();

        String code = "forgotten_password_code";
        ForgottenPasswordEntity forgottenPasswordEntity = mockForgottenPassword(code);
        when(forgottenPasswordDao.findNonExpiredByCode(code)).thenReturn(Optional.of(forgottenPasswordEntity));

        Optional<Errors> errorsOptional = resetPasswordService.updatePassword(jsonNode);

        assertFalse(errorsOptional.isPresent());
    }

    private void setUpJsonNodes(){
        jsonNode = mock(JsonNode.class);
        codeMock = mock(JsonNode.class);
        passwordMock = mock(JsonNode.class);

        when(jsonNode.get("forgotten_password_code")).thenReturn(codeMock);
        when(codeMock.asText()).thenReturn("forgotten_password_code");
        when(jsonNode.get("new_password")).thenReturn(passwordMock);
        when(passwordMock.asText()).thenReturn("valid-password");
    }

    private ForgottenPasswordEntity mockForgottenPassword(String code) {
        UserEntity mockUser = mock(UserEntity.class);
        when(mockUser.getUsername()).thenReturn("random-username");
        return new ForgottenPasswordEntity(code, ZonedDateTime.now(), mockUser);
    }
}
