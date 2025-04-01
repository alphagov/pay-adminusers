package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.app.config.LinksConfig;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ForgottenPasswordEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import jakarta.ws.rs.WebApplicationException;
import java.time.ZonedDateTime;
import java.util.Optional;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ForgottenPasswordServicesTest {

    private static final String SELFSERVICE_URL = "http://selfservice";

    @Mock
    private ForgottenPasswordDao forgottenPasswordDao;
    @Mock
    private UserDao userDao;
    @Mock
    private AdminUsersConfig mockConfig;
    @Mock
    private NotificationService mockNotificationService;

    private ForgottenPasswordServices forgottenPasswordServices;

    @BeforeEach
    public void before() {
        LinksConfig mockLinks = mock(LinksConfig.class);
        when(mockLinks.getSelfserviceUrl()).thenReturn(SELFSERVICE_URL);
        when(mockConfig.getLinks()).thenReturn(mockLinks);
        forgottenPasswordServices = new ForgottenPasswordServices(userDao, forgottenPasswordDao, new LinksBuilder("http://localhost"), mockNotificationService, mockConfig);
    }

    @Test
    public void shouldSendAForgottenPasswordNotification_whenCreating_ifUserFound() {

        ArgumentCaptor<ForgottenPasswordEntity> expectedForgottenPassword = ArgumentCaptor.forClass(ForgottenPasswordEntity.class);

        String username = "existing-user";
        String email = "existing-user@example.com";
        UserEntity mockUser = mock(UserEntity.class);
        when(mockUser.getEmail()).thenReturn(email);
        when(userDao.findByEmail(username)).thenReturn(Optional.of(mockUser));
        when(mockNotificationService.sendForgottenPasswordEmail(eq(email), matches("^http://selfservice/reset-password/[0-9a-z]{32}$")))
                .thenReturn("random-notify-id");
        doNothing().when(forgottenPasswordDao).persist(any(ForgottenPasswordEntity.class));

        forgottenPasswordServices.create(username);

        verify(forgottenPasswordDao).persist(expectedForgottenPassword.capture());
        ForgottenPasswordEntity savedForgottenPassword = expectedForgottenPassword.getValue();
        assertThat(savedForgottenPassword.getUser(), is(mockUser));
        assertThat(savedForgottenPassword.getCode(), is(notNullValue()));
    }

    @Test
    public void shouldStillCreateAForgottenPassword_whenNotificationFails_onCreate_ifUserFound() {

        ArgumentCaptor<ForgottenPasswordEntity> expectedForgottenPassword = ArgumentCaptor.forClass(ForgottenPasswordEntity.class);

        String username = "existing-user";
        String email = "existing-user@example.com";
        UserEntity mockUser = mock(UserEntity.class);
        when(mockUser.getEmail()).thenReturn(email);
        when(userDao.findByEmail(username)).thenReturn(Optional.of(mockUser));
        when(mockNotificationService.sendForgottenPasswordEmail(eq(email), matches("^http://selfservice/reset-password/[0-9a-z]{32}$")))
                .thenThrow(AdminUsersExceptions.userNotificationError(new Exception("Cause")));
        doNothing().when(forgottenPasswordDao).persist(any(ForgottenPasswordEntity.class));

        forgottenPasswordServices.create(username);

        verify(forgottenPasswordDao).persist(expectedForgottenPassword.capture());
        ForgottenPasswordEntity savedForgottenPassword = expectedForgottenPassword.getValue();
        assertThat(savedForgottenPassword.getUser(), is(mockUser));
        assertThat(savedForgottenPassword.getCode(), is(notNullValue()));
    }

    @Test
    public void shouldReturnEmpty_whenCreating_ifUserNotFound() {

        String nonExistentUser = "non-existent-user";
        when(userDao.findByEmail(nonExistentUser)).thenReturn(Optional.empty());

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> forgottenPasswordServices.create(nonExistentUser));
        assertThat(exception.getMessage(), is("HTTP 404 Not Found"));
    }

    @Test
    public void shouldFindForgottenPassword_whenFindByCode_ifFound() {
        String existingCode = "existing-code";
        ForgottenPasswordEntity forgottenPasswordEntity = mockForgottenPassword(existingCode);
        when(forgottenPasswordDao.findNonExpiredByCode(existingCode)).thenReturn(Optional.of(forgottenPasswordEntity));

        Optional<ForgottenPassword> forgottenPasswordOptional = forgottenPasswordServices.findNonExpired(existingCode);
        assertTrue(forgottenPasswordOptional.isPresent());
        assertThat(forgottenPasswordOptional.get().getCode(), is(existingCode));
        assertThat(forgottenPasswordOptional.get().getLinks(), hasSize(1));
    }

    @Test
    public void shouldReturnEmpty_whenFindByCode_ifNotFound() {
        String nonExistentCode = "non-existent-code";
        when(forgottenPasswordDao.findNonExpiredByCode(nonExistentCode)).thenReturn(Optional.empty());

        Optional<ForgottenPassword> forgottenPasswordOptional = forgottenPasswordServices.findNonExpired(nonExistentCode);
        assertFalse(forgottenPasswordOptional.isPresent());
    }

    private ForgottenPasswordEntity mockForgottenPassword(String code) {
        UserEntity mockUser = mock(UserEntity.class);
        return new ForgottenPasswordEntity(code, ZonedDateTime.now(), mockUser);
    }
}
