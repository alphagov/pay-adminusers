package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ForgottenPasswordEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.ws.rs.WebApplicationException;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ResetPasswordServiceTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Mock
    private ForgottenPasswordDao mockForgottenPasswordDao;
    @Mock
    private PasswordHasher mockPasswordHasher;
    @Mock
    private UserDao mockUserDao;

    private ResetPasswordService resetPasswordService;

    @Before
    public void before() throws Exception {
        mockUserDao = mock(UserDao.class);
        mockForgottenPasswordDao = mock(ForgottenPasswordDao.class);
        mockPasswordHasher = mock(PasswordHasher.class);
        resetPasswordService = new ResetPasswordService(mockUserDao, mockForgottenPasswordDao, mockPasswordHasher);
    }

    @Test
    public void shouldThrowAWebApplicationException_whenForgottenPasswordCode_doesNotExistOrIsExpired() {

        String code = "forgottenPasswordCode";
        String password = "myNewPassword";

        when(mockForgottenPasswordDao.findNonExpiredByCode(code)).thenReturn(Optional.empty());

        expectedException.expect(WebApplicationException.class);
        expectedException.expectMessage(is("HTTP 400 Bad Request"));

        resetPasswordService.updatePassword(code, password);
    }

    @Test
    public void shouldUpdatePasswordAsEncrypted_whenCodeIsValid() {

        String code = "forgottenPasswordCode";
        String plainPassword = "myNewPlainPassword";
        String hashedPassword = "hashedPassword";

        ArgumentCaptor<UserEntity> argumentCaptor = ArgumentCaptor.forClass(UserEntity.class);
        UserEntity user = new UserEntity();
        user.setLoginCount(2);
        user.setPassword("whatever");

        ForgottenPasswordEntity forgottenPasswordEntity = new ForgottenPasswordEntity(code, ZonedDateTime.now(), user);
        when(mockForgottenPasswordDao.findNonExpiredByCode(code))
                .thenReturn(Optional.of(forgottenPasswordEntity));
        when(mockPasswordHasher.hash(plainPassword)).thenReturn(hashedPassword);

        resetPasswordService.updatePassword(code, plainPassword);

        verify(mockUserDao).merge(argumentCaptor.capture());

        UserEntity updatedUser = argumentCaptor.getValue();
        assertThat(updatedUser.getLoginCount(), is(0));
        assertThat(updatedUser.getPassword(), is(hashedPassword));

        verify(mockForgottenPasswordDao).remove(forgottenPasswordEntity);
    }
}
