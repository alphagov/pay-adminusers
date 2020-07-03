package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ForgottenPasswordEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ResetPasswordServiceTest {

    @Mock
    private ForgottenPasswordDao mockForgottenPasswordDao;
    @Mock
    private PasswordHasher mockPasswordHasher;
    @Mock
    private UserDao mockUserDao;

    private ResetPasswordService resetPasswordService;

    @BeforeEach
    public void before() {
        mockUserDao = mock(UserDao.class);
        mockForgottenPasswordDao = mock(ForgottenPasswordDao.class);
        mockPasswordHasher = mock(PasswordHasher.class);
        resetPasswordService = new ResetPasswordService(mockUserDao, mockForgottenPasswordDao, mockPasswordHasher);
    }

    @Test
    public void shouldReturnOptionalEmpty_whenForgottenPasswordCode_doesNotExistOrIsExpired() {

        String code = "forgottenPasswordCode";
        String password = "myNewPassword";

        when(mockForgottenPasswordDao.findNonExpiredByCode(code)).thenReturn(Optional.empty());

        Optional<Integer> userIdOptional = resetPasswordService.updatePassword(code, password);

        assertThat(userIdOptional.isPresent(), is(false));
    }

    @Test
    public void shouldUpdatePasswordAsEncrypted_whenCodeIsValid() {

        String code = "forgottenPasswordCode";
        String plainPassword = "myNewPlainPassword";
        String hashedPassword = "hashedPassword";
        int userId = 666;

        ArgumentCaptor<UserEntity> argumentCaptor = ArgumentCaptor.forClass(UserEntity.class);
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setLoginCounter(2);
        user.setPassword("whatever");

        ForgottenPasswordEntity forgottenPasswordEntity = new ForgottenPasswordEntity(code, ZonedDateTime.now(), user);
        when(mockForgottenPasswordDao.findNonExpiredByCode(code))
                .thenReturn(Optional.of(forgottenPasswordEntity));
        when(mockPasswordHasher.hash(plainPassword)).thenReturn(hashedPassword);

        Optional<Integer> userIdOptional = resetPasswordService.updatePassword(code, plainPassword);
        assertThat(userIdOptional.isPresent(), is(true));
        assertThat(userIdOptional.get(), is(userId));

        verify(mockUserDao).merge(argumentCaptor.capture());

        UserEntity updatedUser = argumentCaptor.getValue();
        assertThat(updatedUser.getLoginCounter(), is(0));
        assertThat(updatedUser.getPassword(), is(hashedPassword));

        verify(mockForgottenPasswordDao).remove(forgottenPasswordEntity);
    }
}
