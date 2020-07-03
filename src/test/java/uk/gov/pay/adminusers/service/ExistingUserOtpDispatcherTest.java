package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.SecondFactorToken;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.time.ZonedDateTime;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.persistence.entity.UTCDateTimeConverter.UTC;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.CHANGE_SIGN_IN_2FA_TO_SMS;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.SIGN_IN;

@ExtendWith(MockitoExtension.class)
public class ExistingUserOtpDispatcherTest {

    private static final String USER_EXTERNAL_ID = "7d19aff33f8948deb97ed16b2912dcd3";
    private static final String USER_USERNAME = "random-name";

    @Mock
    private UserDao userDao;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SecondFactorAuthenticator secondFactorAuthenticator;

    private ExistingUserOtpDispatcher existingUserOtpDispatcher;

    @BeforeEach
    public void before() {
        existingUserOtpDispatcher = new ExistingUserOtpDispatcher(() -> notificationService, secondFactorAuthenticator, userDao);
    }

    @Test
    public void shouldSendSignInOtpIfUserFound() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.newPassCode(user.getOtpKey())).thenReturn(123456);
        when(notificationService.sendSecondFactorPasscodeSms(any(String.class), eq("123456"), eq(SIGN_IN))).thenReturn("random-notify-id");

        Optional<SecondFactorToken> tokenOptional = existingUserOtpDispatcher.sendSignInOtp(user.getExternalId());

        assertTrue(tokenOptional.isPresent());
        assertThat(tokenOptional.get().getPasscode(), is("123456"));
    }

    @Test
    public void shouldPadSignInOtpToSixDigits() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.newPassCode(user.getOtpKey())).thenReturn(12345);
        when(notificationService.sendSecondFactorPasscodeSms(any(String.class), eq("012345"), eq(SIGN_IN))).thenReturn("random-notify-id");

        Optional<SecondFactorToken> tokenOptional = existingUserOtpDispatcher.sendSignInOtp(user.getExternalId());

        assertTrue(tokenOptional.isPresent());
        assertThat(tokenOptional.get().getPasscode(), is("012345"));
    }

    @Test
    public void shouldGracefullyHandleNotifyErrorSendingSignInOtp() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.newPassCode(user.getOtpKey())).thenReturn(654321);

        when(notificationService.sendSecondFactorPasscodeSms(any(String.class), eq("654321"), eq(SIGN_IN)))
                .thenThrow(AdminUsersExceptions.userNotificationError());

        Optional<SecondFactorToken> tokenOptional = existingUserOtpDispatcher.sendSignInOtp(user.getExternalId());

        assertTrue(tokenOptional.isPresent());
        assertThat(tokenOptional.get().getPasscode(), is("654321"));
    }

    @Test
    public void shouldNotSendSignInOtpIfUserDoesNotExist() {
        String nonExistentExternalId = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        when(userDao.findByExternalId(nonExistentExternalId)).thenReturn(Optional.empty());

        Optional<SecondFactorToken> tokenOptional = existingUserOtpDispatcher.sendSignInOtp(nonExistentExternalId);

        assertFalse(tokenOptional.isPresent());
    }

    @Test
    public void shouldSendChangeSignInMethodOtpIfUserFound() {
        User user = aUserWithProvisionalOtpKey();
        UserEntity userEntity = UserEntity.from(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.newPassCode(user.getProvisionalOtpKey())).thenReturn(654321);
        when(notificationService.sendSecondFactorPasscodeSms(any(String.class), eq("654321"), eq(CHANGE_SIGN_IN_2FA_TO_SMS)))
                .thenReturn("random-notify-id");

        Optional<SecondFactorToken> tokenOptional = existingUserOtpDispatcher.sendChangeSignMethodToSmsOtp(user.getExternalId());

        assertTrue(tokenOptional.isPresent());
        assertThat(tokenOptional.get().getPasscode(), is("654321"));

        verify(notificationService, never()).sendSecondFactorPasscodeSms(any(String.class), eq(user.getOtpKey()),
                any(NotificationService.OtpNotifySmsTemplateId.class));
    }

    @Test
    public void shouldPadChangeSignInMethodOtpToSixDigits() {
        User user = aUserWithProvisionalOtpKey();
        UserEntity userEntity = UserEntity.from(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.newPassCode(user.getProvisionalOtpKey())).thenReturn(12345);
        when(notificationService.sendSecondFactorPasscodeSms(any(String.class), eq("012345"), eq(CHANGE_SIGN_IN_2FA_TO_SMS)))
                .thenReturn("random-notify-id");

        Optional<SecondFactorToken> tokenOptional = existingUserOtpDispatcher.sendChangeSignMethodToSmsOtp(user.getExternalId());

        assertTrue(tokenOptional.isPresent());
        assertThat(tokenOptional.get().getPasscode(), is("012345"));
    }

    @Test
    public void shouldNotSendChangeSignInOtpIfProvisionalOtpKeyNotSet() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));

        Optional<SecondFactorToken> tokenOptional = existingUserOtpDispatcher.sendChangeSignMethodToSmsOtp(user.getExternalId());

        assertFalse(tokenOptional.isPresent());

        verifyNoInteractions(secondFactorAuthenticator);
    }

    @Test
    public void shouldGracefullyHandleNotifyErrorSendingChangeSignInOtp() {
        User user = aUserWithProvisionalOtpKey();
        UserEntity userEntity = UserEntity.from(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.newPassCode(user.getProvisionalOtpKey())).thenReturn(654321);

        when(notificationService.sendSecondFactorPasscodeSms(any(String.class), eq("654321"), eq(CHANGE_SIGN_IN_2FA_TO_SMS)))
                .thenThrow(AdminUsersExceptions.userNotificationError());

        Optional<SecondFactorToken> tokenOptional = existingUserOtpDispatcher.sendChangeSignMethodToSmsOtp(user.getExternalId());

        assertTrue(tokenOptional.isPresent());
        assertThat(tokenOptional.get().getPasscode(), is("654321"));
    }

    @Test
    public void shouldNotSendChangeSignInOtpIfUserDoesNotExist() {
        String nonExistentExternalId = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        when(userDao.findByExternalId(nonExistentExternalId)).thenReturn(Optional.empty());

        Optional<SecondFactorToken> tokenOptional = existingUserOtpDispatcher.sendChangeSignMethodToSmsOtp(nonExistentExternalId);

        assertFalse(tokenOptional.isPresent());
    }

    private User aUser() {
        return User.from(randomInt(), USER_EXTERNAL_ID, USER_USERNAME, "random-password",
                "user@test.test","784rh", "07700900000", emptyList(),
                null, SecondFactorMethod.SMS,null, null, null);
    }

    private User aUserWithProvisionalOtpKey() {
        return User.from(randomInt(), USER_EXTERNAL_ID, USER_USERNAME, "random-password",
                "user@test.test","784rh", "07700900001", emptyList(),
                null, SecondFactorMethod.APP,"provisional OTP key", ZonedDateTime.now(UTC), null);
    }

}
