package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.SecondFactorToken;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.LEGACY;

@RunWith(MockitoJUnitRunner.class)
public class ExistingUserOtpDispatcherTest {

    private static final String USER_EXTERNAL_ID = "7d19aff33f8948deb97ed16b2912dcd3";
    private static final String USER_USERNAME = "random-name";

    @Mock
    private UserDao userDao;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SecondFactorAuthenticator secondFactorAuthenticator;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private ExistingUserOtpDispatcher existingUserOtpDispatcher;

    @Before
    public void before() {
        existingUserOtpDispatcher = new ExistingUserOtpDispatcher(() -> notificationService, secondFactorAuthenticator, userDao);
    }

    @Test
    public void shouldReturn2FAToken_whenCreate2FA_ifUserFound() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.newPassCode(user.getOtpKey())).thenReturn(123456);
        when(notificationService.sendSecondFactorPasscodeSms(any(String.class), eq("123456"), eq(LEGACY)))
                .thenReturn("random-notify-id");

        Optional<SecondFactorToken> tokenOptional = existingUserOtpDispatcher.newSecondFactorPasscode(user.getExternalId(), false);

        assertTrue(tokenOptional.isPresent());
        assertThat(tokenOptional.get().getPasscode(), is("123456"));
    }

    @Test
    public void shouldZeroPad2FATokenTo6Digits_whenCreate2FA() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.newPassCode(user.getOtpKey())).thenReturn(12345);
        when(notificationService.sendSecondFactorPasscodeSms(any(String.class), eq("012345"), eq(LEGACY)))
                .thenReturn("random-notify-id");

        Optional<SecondFactorToken> tokenOptional = existingUserOtpDispatcher.newSecondFactorPasscode(user.getExternalId(), false);

        assertTrue(tokenOptional.isPresent());
        assertThat(tokenOptional.get().getPasscode(), is("012345"));
    }

    @Test
    public void shouldReturn2FAToken_whenCreate2FA_evenIfNotifyThrowsAnError() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.newPassCode(user.getOtpKey())).thenReturn(123456);

        when(notificationService.sendSecondFactorPasscodeSms(any(String.class), eq("123456"), eq(LEGACY)))
                .thenThrow(AdminUsersExceptions.userNotificationError());

        Optional<SecondFactorToken> tokenOptional = existingUserOtpDispatcher.newSecondFactorPasscode(user.getExternalId(), false);

        assertTrue(tokenOptional.isPresent());
        assertThat(tokenOptional.get().getPasscode(), is("123456"));
    }

    @Test
    public void shouldReturnEmpty_whenCreate2FA_ifUserNotFound() {
        String nonExistentExternalId = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        when(userDao.findByExternalId(nonExistentExternalId)).thenReturn(Optional.empty());

        Optional<SecondFactorToken> tokenOptional = existingUserOtpDispatcher.newSecondFactorPasscode(nonExistentExternalId, false);

        assertFalse(tokenOptional.isPresent());
    }

    @Test
    public void shouldReturn2FAToken_whenCreate2FA_withProvisionalOtpKey_ifUserFound() {
        User user = aUser();
        user.setProvisionalOtpKey("provisional OTP key");
        UserEntity userEntity = UserEntity.from(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.newPassCode(user.getProvisionalOtpKey())).thenReturn(654321);
        when(notificationService.sendSecondFactorPasscodeSms(any(String.class), eq("654321"), eq(LEGACY)))
                .thenReturn("random-notify-id");

        Optional<SecondFactorToken> tokenOptional = existingUserOtpDispatcher.newSecondFactorPasscode(user.getExternalId(), true);

        assertTrue(tokenOptional.isPresent());
        assertThat(tokenOptional.get().getPasscode(), is("654321"));

        verify(notificationService, never()).sendSecondFactorPasscodeSms(any(String.class), eq(user.getOtpKey()), eq(LEGACY));
    }

    @Test
    public void shouldReturn2FAToken_whenCreate2FA_withProvisionalOtpKey_ifProvisionalOtpKeyNotSet() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));

        Optional<SecondFactorToken> tokenOptional = existingUserOtpDispatcher.newSecondFactorPasscode(user.getExternalId(), true);

        assertFalse(tokenOptional.isPresent());

        verifyNoInteractions(secondFactorAuthenticator);
    }

    private User aUser() {
        return User.from(randomInt(), USER_EXTERNAL_ID, USER_USERNAME, "random-password",
                "email@example.com","784rh", "8948924", emptyList(),
                null, SecondFactorMethod.SMS,null, null, null);
    }

}
