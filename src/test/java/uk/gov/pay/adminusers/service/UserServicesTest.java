package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.model.PatchRequest;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.SecondFactorToken;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.pay.commons.model.SupportedLanguage;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.Permission.permission;
import static uk.gov.pay.adminusers.model.Role.role;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.LEGACY;

@RunWith(MockitoJUnitRunner.class)
public class UserServicesTest {

    @Mock
    private UserDao userDao;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SecondFactorAuthenticator secondFactorAuthenticator;
    @Captor
    private ArgumentCaptor<UserEntity> userEntityArgumentCaptor;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private UserServices userServices;

    private static final String USER_EXTERNAL_ID = "7d19aff33f8948deb97ed16b2912dcd3";
    private static final String USER_USERNAME = "random-name";
    private static final String ANOTHER_USER_EXTERNAL_ID = "7d19aff33f8948deb97ed16b2912dcd4";
    private static final String ANOTHER_USER_USERNAME = "another-random-name";

    @Before
    public void before() {
        userServices = new UserServices(userDao, passwordHasher,
                new LinksBuilder("http://localhost"), 3,
                () -> notificationService, secondFactorAuthenticator);
    }

    @Test
    public void shouldFindAUserByExternalId() {
        User user = aUser();

        UserEntity userEntity = aUserEntityWithTrimmings(user);

        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(userDao.findByExternalId(USER_EXTERNAL_ID)).thenReturn(userEntityOptional);

        Optional<User> userOptional = userServices.findUserByExternalId(USER_EXTERNAL_ID);
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getExternalId(), is(USER_EXTERNAL_ID));
    }

    @Test
    public void shouldFindAUsersByExternalIds() {
        User user1 = aUser();
        User user2 = anotherUser();

        UserEntity userEntity1 = aUserEntityWithTrimmings(user1);
        UserEntity userEntity2 = aUserEntityWithTrimmings(user2);

        when(userDao.findByExternalIds(Arrays.asList(user1.getExternalId(), user2.getExternalId()))).thenReturn(Arrays.asList(userEntity1, userEntity2));

        List<User> users = userServices.findUsersByExternalIds(Arrays.asList(user1.getExternalId(), user2.getExternalId()));
        assertThat(users.size(), is(2));

        assertThat(users.get(0).getExternalId(), is(user1.getExternalId()));
        assertThat(users.get(1).getExternalId(), is(user2.getExternalId()));
    }

    @Test
    public void shouldReturnEmpty_WhenFindByExternalId_ifNotFound() {
        when(userDao.findByExternalId(USER_EXTERNAL_ID)).thenReturn(Optional.empty());

        Optional<User> userOptional = userServices.findUserByExternalId(USER_EXTERNAL_ID);
        assertFalse(userOptional.isPresent());
    }

    @Test
    public void shouldFindAUserByUserName() {
        User user = aUser();

        UserEntity userEntity = aUserEntityWithTrimmings(user);

        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(userDao.findByUsername(USER_USERNAME)).thenReturn(userEntityOptional);

        Optional<User> userOptional = userServices.findUserByUsername(USER_USERNAME);
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getUsername(), is(USER_USERNAME));
    }

    @Test
    public void shouldReturnEmpty_WhenFindByUserName_ifNotFound() {
        when(userDao.findByUsername(USER_USERNAME)).thenReturn(Optional.empty());

        Optional<User> userOptional = userServices.findUserByUsername(USER_USERNAME);
        assertFalse(userOptional.isPresent());
    }

    @Test
    public void shouldReturnUserAndResetLoginCount_ifAuthenticationSuccessfulAndUserNotDisabled() {
        User user = aUser();
        user.setLoginCounter(2);

        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setPassword("hashed-password");

        when(passwordHasher.isEqual("random-password", "hashed-password")).thenReturn(true);
        when(userDao.findByUsername(USER_USERNAME)).thenReturn(Optional.of(userEntity));
        when(userDao.merge(userEntityArgumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> userOptional = userServices.authenticate(USER_USERNAME, "random-password");
        assertTrue(userOptional.isPresent());

        User authenticatedUser = userOptional.get();
        assertThat(authenticatedUser.getUsername(), is(USER_USERNAME));
        assertThat(authenticatedUser.getLinks().size(), is(1));
        assertThat(userEntityArgumentCaptor.getValue().getLoginCounter(), is(0));
    }

    @Test
    public void shouldReturnUserAndNotResetLoginCount_ifAuthenticationSuccessfulButUserDisabled() {
        User user = aUser();
        user.setLoginCounter(2);
        user.setDisabled(true);

        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setPassword("hashed-password");

        when(passwordHasher.isEqual("random-password", "hashed-password")).thenReturn(true);
        when(userDao.findByUsername(USER_USERNAME)).thenReturn(Optional.of(userEntity));

        Optional<User> userOptional = userServices.authenticate(USER_USERNAME, "random-password");
        assertTrue(userOptional.isPresent());

        User authenticatedUser = userOptional.get();
        assertThat(authenticatedUser.getUsername(), is(USER_USERNAME));
        assertThat(authenticatedUser.isDisabled(), is(true));
        assertThat(authenticatedUser.getLinks().size(), is(1));
        assertThat(userEntity.getLoginCounter(), is(2));
    }

    @Test
    public void shouldReturnEmptyAndIncrementLoginCount_ifAuthenticationFail() {
        User user = aUser();
        user.setLoginCounter(1);
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setPassword("hashed-password");

        when(userDao.findByUsername(USER_USERNAME)).thenReturn(Optional.of(UserEntity.from(user)));
        when(userDao.merge(userEntityArgumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> userOptional = userServices.authenticate(USER_USERNAME, "random-password");
        assertFalse(userOptional.isPresent());

        UserEntity savedUser = userEntityArgumentCaptor.getValue();
        assertTrue(within(3, SECONDS, savedUser.getCreatedAt()).matches(savedUser.getUpdatedAt()));
        assertThat(savedUser.getLoginCounter(), is(2));
        assertThat(savedUser.isDisabled(), is(false));
    }

    @Test
    public void shouldLockUser_onTooManyAuthFailures() {
        User user = aUser();
        user.setLoginCounter(2);
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setPassword("hashed-password");

        when(userDao.findByUsername(USER_USERNAME)).thenReturn(Optional.of(UserEntity.from(user)));
        when(userDao.merge(userEntityArgumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        userServices.authenticate(USER_USERNAME, "random-password");
        UserEntity savedUser = userEntityArgumentCaptor.getValue();
        assertTrue(within(3, SECONDS, savedUser.getCreatedAt()).matches(savedUser.getUpdatedAt()));
        assertThat(savedUser.getLoginCounter(), is(3));
        assertThat(savedUser.isDisabled(), is(true));
    }

    @Test
    public void shouldReturnUser_whenIncrementingSessionVersion_ifUserFound() {
        User user = aUser();

        JsonNode node = new ObjectMapper().valueToTree(Map.of("path", "sessionVersion", "op", "append", "value", "2"));
        UserEntity userEntity = aUserEntityWithTrimmings(user);

        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(userDao.findByExternalId(USER_EXTERNAL_ID)).thenReturn(userEntityOptional);

        Optional<User> userOptional = userServices.patchUser(USER_EXTERNAL_ID, PatchRequest.from(node));
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getExternalId(), is(USER_EXTERNAL_ID));
        assertThat(userOptional.get().getSessionVersion(), is(2));
    }

    @Test
    public void shouldReturnUser_withDisabled_ifUserFoundDuringPatch() {
        User user = aUser();

        JsonNode node = new ObjectMapper().valueToTree(Map.of("path", "disabled", "op", "replace", "value", "true"));

        UserEntity userEntity = aUserEntityWithTrimmings(user);

        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(userDao.findByExternalId(USER_EXTERNAL_ID)).thenReturn(userEntityOptional);

        assertFalse(user.isDisabled());

        Optional<User> userOptional = userServices.patchUser(USER_EXTERNAL_ID, PatchRequest.from(node));
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getExternalId(), is(USER_EXTERNAL_ID));
        assertTrue(userOptional.get().isDisabled());
    }

    @Test
    public void shouldResetLoginCounter_whenTheUserIsEnabled() {
        User user = aUser();
        user.setDisabled(Boolean.TRUE);
        user.setLoginCounter(11);

        JsonNode node = new ObjectMapper().valueToTree(Map.of("path", "disabled", "op", "replace", "value", "false"));
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(userDao.findByExternalId(USER_EXTERNAL_ID)).thenReturn(userEntityOptional);

        assertTrue(user.isDisabled());
        assertThat(user.getLoginCounter(), is(11));

        Optional<User> userOptional = userServices.patchUser(USER_EXTERNAL_ID, PatchRequest.from(node));
        assertTrue(userOptional.isPresent());

        assertFalse(userOptional.get().isDisabled());
        assertThat(userOptional.get().getLoginCounter(), is(0));
    }

    @Test
    public void shouldUpdateTelephoneNumber_whenReplacingTelephoneNumber_ifUserFound() {
        User user = aUser();
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setTelephoneNumber("+447700900000");

        String newTelephoneNumber = "+441134960000";
        JsonNode node = new ObjectMapper().valueToTree(Map.of("path", "telephone_number", "op", "replace", "value", newTelephoneNumber));
        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);

        when(userDao.findByExternalId(USER_EXTERNAL_ID)).thenReturn(userEntityOptional);

        Optional<User> userOptional = userServices.patchUser(USER_EXTERNAL_ID, PatchRequest.from(node));

        verify(userDao, times(1)).merge(userEntityArgumentCaptor.capture());

        UserEntity persistedUser = userEntityArgumentCaptor.getValue();
        assertThat(persistedUser.getTelephoneNumber(), is(newTelephoneNumber));
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getTelephoneNumber(), is(newTelephoneNumber));
    }

    @Test
    public void shouldUpdateFeatures_whenPathIsFeatures_ifUserFound() {
        User user = aUser();
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setFeatures("1");

        String newFeature = "1,2,3";
        JsonNode node = new ObjectMapper().valueToTree(Map.of("path", "features", "op", "replace", "value", newFeature));
        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);

        when(userDao.findByExternalId(USER_EXTERNAL_ID)).thenReturn(userEntityOptional);

        Optional<User> userOptional = userServices.patchUser(USER_EXTERNAL_ID, PatchRequest.from(node));

        verify(userDao, times(1)).merge(userEntityArgumentCaptor.capture());

        UserEntity persistedUser = userEntityArgumentCaptor.getValue();
        assertThat(persistedUser.getFeatures(), is(newFeature));
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getFeatures(), is(newFeature));
    }

    @Test
    public void shouldReturn2FAToken_whenCreate2FA_ifUserFound() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.newPassCode(user.getOtpKey())).thenReturn(123456);
        when(notificationService.sendSecondFactorPasscodeSms(any(String.class), eq("123456"), eq(LEGACY)))
                .thenReturn("random-notify-id");

        Optional<SecondFactorToken> tokenOptional = userServices.newSecondFactorPasscode(user.getExternalId(), false);

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

        Optional<SecondFactorToken> tokenOptional = userServices.newSecondFactorPasscode(user.getExternalId(), false);

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

        Optional<SecondFactorToken> tokenOptional = userServices.newSecondFactorPasscode(user.getExternalId(), false);

        assertTrue(tokenOptional.isPresent());
        assertThat(tokenOptional.get().getPasscode(), is("123456"));
    }

    @Test
    public void shouldReturnEmpty_whenCreate2FA_ifUserNotFound() {
        String nonExistentExternalId = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        when(userDao.findByExternalId(nonExistentExternalId)).thenReturn(Optional.empty());

        Optional<SecondFactorToken> tokenOptional = userServices.newSecondFactorPasscode(nonExistentExternalId, false);

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

        Optional<SecondFactorToken> tokenOptional = userServices.newSecondFactorPasscode(user.getExternalId(), true);

        assertTrue(tokenOptional.isPresent());
        assertThat(tokenOptional.get().getPasscode(), is("654321"));

        verify(notificationService, never()).sendSecondFactorPasscodeSms(any(String.class), eq(user.getOtpKey()), eq(LEGACY));
    }

    @Test
    public void shouldReturn2FAToken_whenCreate2FA_withProvisionalOtpKey_ifProvisionalOtpKeyNotSet() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));

        Optional<SecondFactorToken> tokenOptional = userServices.newSecondFactorPasscode(user.getExternalId(), true);

        assertFalse(tokenOptional.isPresent());

        verifyZeroInteractions(secondFactorAuthenticator);
    }

    @Test
    public void shouldReturnUser_whenAuthenticate2FA_ifSuccessful() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        User aUser = aUser();
        int newPassCode = 123456;
        UserEntity userEntity = aUserEntityWithTrimmings(aUser);
        userEntity.setLastLoggedInAt(now);
        when(userDao.findByExternalId(aUser.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.authorize(aUser.getOtpKey(), newPassCode)).thenReturn(true);

        Optional<User> userOptional = userServices.authenticateSecondFactor(aUser.getExternalId(), newPassCode);

        assertTrue(userOptional.isPresent());
        User user = userOptional.get();
        assertThat(user.getExternalId(), is(aUser.getExternalId()));
        assertThat(user.getLoginCounter(), is(0));
        assertThat(user.getLastLoggedInAt().isAfter(ZonedDateTime.now().minusSeconds(10)), is(true));
    }

    @Test
    public void shouldReturnEmpty_whenAuthenticate2FA_ifUnsuccessful_whenTheUserNeverLoggedIn() {
        User user = aUser();
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.authorize(user.getOtpKey(), 123456)).thenReturn(false);
        when(userDao.merge(userEntityArgumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> tokenOptional = userServices.authenticateSecondFactor(user.getExternalId(), 123456);

        assertFalse(tokenOptional.isPresent());

        UserEntity savedUser = userEntityArgumentCaptor.getValue();
        assertThat(savedUser.getLoginCounter(), is(1));
        assertThat(savedUser.isDisabled(), is(false));
        assertThat(savedUser.getLastLoggedInAt(), is(nullValue()));
    }

    @Test
    public void shouldReturnEmpty_whenAuthenticate2FA_ifUnsuccessful_whenTheUserLoggedInAtLeastOnce() {
        ZonedDateTime lastLoggedInDateTime = ZonedDateTime.now(ZoneId.of("UTC")).minusDays(7);
        User user = aUser();
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setLastLoggedInAt(lastLoggedInDateTime);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.authorize(user.getOtpKey(), 123456)).thenReturn(false);
        when(userDao.merge(userEntityArgumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> tokenOptional = userServices.authenticateSecondFactor(user.getExternalId(), 123456);

        assertFalse(tokenOptional.isPresent());

        UserEntity savedUser = userEntityArgumentCaptor.getValue();
        assertThat(savedUser.getLoginCounter(), is(1));
        assertThat(savedUser.isDisabled(), is(false));
        assertThat(savedUser.getLastLoggedInAt().equals(lastLoggedInDateTime), is(true));
    }

    @Test
    public void shouldReturnEmptyAndDisable_whenAuthenticate2FA_ifUnsuccessfulMaxRetry() {
        User user = aUser();
        user.setLoginCounter(3);
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.authorize(user.getOtpKey(), 123456)).thenReturn(false);
        when(userDao.merge(userEntityArgumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> tokenOptional = userServices.authenticateSecondFactor(user.getExternalId(), 123456);

        assertFalse(tokenOptional.isPresent());

        UserEntity savedUser = userEntityArgumentCaptor.getValue();
        assertThat(savedUser.getLoginCounter(), is(4));
        assertThat(savedUser.isDisabled(), is(true));
    }

    @Test
    public void shouldReturnEmpty_whenAuthenticate2FA_ifUserNotFound() {
        String nonExistentExternalId = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        when(userDao.findByExternalId(nonExistentExternalId)).thenReturn(Optional.empty());

        Optional<User> tokenOptional = userServices.authenticateSecondFactor(nonExistentExternalId, 111111);

        assertFalse(tokenOptional.isPresent());
    }

    @Test
    public void shouldReturnUser_whenProvisionNewOtpKey_ifUserFound() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setOtpKey("Original OTP key");

        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.generateNewBase32EncodedSecret()).thenReturn("Provisional OTP key");

        Optional<User> result = userServices.provisionNewOtpKey(user.getExternalId());

        assertThat(result.get().getOtpKey(), is("Original OTP key"));
        assertThat(result.get().getProvisionalOtpKey(), is("Provisional OTP key"));
        assertTrue(within(3, SECONDS, result.get().getProvisionalOtpKeyCreatedAt()).matches(ZonedDateTime.now(ZoneOffset.UTC)));
        
        verify(userDao).merge(userEntityArgumentCaptor.capture());
        UserEntity savedUser = userEntityArgumentCaptor.getValue();
        assertThat(savedUser.getOtpKey(), is("Original OTP key"));
        assertThat(savedUser.getProvisionalOtpKey(), is("Provisional OTP key"));
        assertTrue(within(3, SECONDS, savedUser.getProvisionalOtpKeyCreatedAt()).matches(ZonedDateTime.now(ZoneOffset.UTC)));
    }

    @Test
    public void shouldReturnEmpty_whenProvisionNewOtpKey_ifUserNotFound() {
        String nonExistentExternalId = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        when(userDao.findByExternalId(nonExistentExternalId)).thenReturn(Optional.empty());

        Optional<User> result = userServices.provisionNewOtpKey(nonExistentExternalId);

        assertFalse(result.isPresent());

        verify(userDao, never()).merge(any(UserEntity.class));
    }

    @Test
    public void shouldReturnEmpty_whenProvisionNewOtpKey_ifUserDisabled() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setOtpKey("Original OTP key");
        userEntity.setDisabled(true);

        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));

        Optional<User> result = userServices.provisionNewOtpKey(user.getExternalId());

        assertFalse(result.isPresent());

        assertThat(userEntity.getOtpKey(), is("Original OTP key"));
        assertThat(userEntity.getProvisionalOtpKey(), is(nullValue()));
        assertThat(userEntity.getProvisionalOtpKeyCreatedAt(), is(nullValue()));

        verify(userDao, never()).merge(any(UserEntity.class));
    }

    @Test
    public void shouldReturnUser_whenActivateNewOtpKey_ifUserFound() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setSecondFactor(SecondFactorMethod.SMS);
        userEntity.setOtpKey("Original OTP key");
        userEntity.setProvisionalOtpKey("New OTP key");
        userEntity.setProvisionalOtpKeyCreatedAt(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(89));

        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.authorize("New OTP key", 123456)).thenReturn(true);

        Optional<User> result = userServices.activateNewOtpKey(user.getExternalId(), SecondFactorMethod.APP, 123456);

        assertThat(result.get().getOtpKey(), is("New OTP key"));
        assertThat(result.get().getProvisionalOtpKey(), is(nullValue()));
        assertThat(result.get().getProvisionalOtpKeyCreatedAt(), is(nullValue()));
        
        verify(userDao).merge(userEntityArgumentCaptor.capture());
        UserEntity savedUser = userEntityArgumentCaptor.getValue();
        assertThat(savedUser.getOtpKey(), is("New OTP key"));
        assertThat(savedUser.getProvisionalOtpKey(), is(nullValue()));
        assertThat(savedUser.getProvisionalOtpKey(), is(nullValue()));
    }

    @Test
    public void shouldReturnUser_whenActivateNewOtpKey_ifCodeIncorrect() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setSecondFactor(SecondFactorMethod.SMS);
        userEntity.setOtpKey("Original OTP key");
        userEntity.setProvisionalOtpKey("New OTP key");
        userEntity.setProvisionalOtpKeyCreatedAt(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(89));

        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.authorize("New OTP key", 123456)).thenReturn(false);

        Optional<User> result = userServices.activateNewOtpKey(user.getExternalId(), SecondFactorMethod.APP, 123456);

        assertFalse(result.isPresent());

        assertThat(userEntity.getOtpKey(), is("Original OTP key"));
        assertThat(userEntity.getSecondFactor(), is(SecondFactorMethod.SMS));

        verify(userDao, never()).merge(any(UserEntity.class));
    }

    @Test
    public void shouldReturnUser_whenActivateNewOtpKey_ifNoProvisionalOtpCode() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setSecondFactor(SecondFactorMethod.SMS);
        userEntity.setOtpKey("Original OTP key");
        userEntity.setProvisionalOtpKeyCreatedAt(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(89));

        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));

        Optional<User> result = userServices.activateNewOtpKey(user.getExternalId(), SecondFactorMethod.APP, 123456);

        assertFalse(result.isPresent());

        assertThat(userEntity.getOtpKey(), is("Original OTP key"));
        assertThat(userEntity.getSecondFactor(), is(SecondFactorMethod.SMS));

        verify(userDao, never()).merge(any(UserEntity.class));
    }

    @Test
    public void shouldReturnUser_whenActivateNewOtpKey_ifNoProvisionalOtpCodeCreatedAt() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setSecondFactor(SecondFactorMethod.SMS);
        userEntity.setOtpKey("Original OTP key");
        userEntity.setProvisionalOtpKey("New OTP key");

        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));

        Optional<User> result = userServices.activateNewOtpKey(user.getExternalId(), SecondFactorMethod.APP, 123456);

        assertFalse(result.isPresent());

        assertThat(userEntity.getOtpKey(), is("Original OTP key"));
        assertThat(userEntity.getSecondFactor(), is(SecondFactorMethod.SMS));

        verify(userDao, never()).merge(any(UserEntity.class));
    }

    @Test
    public void shouldReturnUser_whenActivateNewOtpKey_ifProvisionalOtpCodeCreatedAtTooLongAgo() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setSecondFactor(SecondFactorMethod.SMS);
        userEntity.setOtpKey("Original OTP key");
        userEntity.setProvisionalOtpKey("New OTP key");
        userEntity.setProvisionalOtpKeyCreatedAt(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(91));

        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));

        Optional<User> result = userServices.activateNewOtpKey(user.getExternalId(), SecondFactorMethod.APP, 123456);

        assertFalse(result.isPresent());

        assertThat(userEntity.getOtpKey(), is("Original OTP key"));
        assertThat(userEntity.getSecondFactor(), is(SecondFactorMethod.SMS));

        verify(userDao, never()).merge(any(UserEntity.class));
    }

    @Test
    public void shouldReturnEmpty_whenActivateNewOtpKey_ifUserNotFound() {
        String nonExistentExternalId = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        when(userDao.findByExternalId(nonExistentExternalId)).thenReturn(Optional.empty());

        Optional<User> result = userServices.activateNewOtpKey(nonExistentExternalId, SecondFactorMethod.SMS, 123456);

        assertFalse(result.isPresent());

        verify(userDao, never()).merge(any(UserEntity.class));
    }

    @Test
    public void shouldReturnUser_whenActivateNewOtpKey_ifUserDisabled() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setSecondFactor(SecondFactorMethod.SMS);
        userEntity.setOtpKey("Original OTP key");
        userEntity.setProvisionalOtpKey("New OTP key");
        userEntity.setProvisionalOtpKeyCreatedAt(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(89));
        userEntity.setDisabled(true);

        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));

        Optional<User> result = userServices.activateNewOtpKey(user.getExternalId(), SecondFactorMethod.APP, 123456);

        assertFalse(result.isPresent());

        assertThat(userEntity.getOtpKey(), is("Original OTP key"));
        assertThat(userEntity.getSecondFactor(), is(SecondFactorMethod.SMS));

        verify(userDao, never()).merge(any(UserEntity.class));
    }

    @Test
    public void resetSecondFactor_shouldUpdateOtpMethodAndSetNewOtpKey_ifSecondFactorMethodIsApp() {
        User user = aUser();
        user.setSecondFactor(SecondFactorMethod.APP);
        UserEntity userEntity = UserEntity.from(user);

        String newOtpKey = "newOtpKey";
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.generateNewBase32EncodedSecret()).thenReturn(newOtpKey);

        Optional<User> result = userServices.resetSecondFactor(user.getExternalId());

        verify(userDao, times(1)).merge(userEntityArgumentCaptor.capture());

        UserEntity persistedUser = userEntityArgumentCaptor.getValue();
        
        assertThat(result.isPresent(), is(true));
        assertThat(persistedUser.getOtpKey(), is(newOtpKey));
        assertThat(persistedUser.getSecondFactor(), is(SecondFactorMethod.SMS));
    }

    @Test
    public void resetSecondFactor_shouldNotUpdateUser_ifSecondFactorMethodIsSms() {
        User user = aUser();
        user.setSecondFactor(SecondFactorMethod.SMS);
        UserEntity userEntity = UserEntity.from(user);

        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));

        Optional<User> result = userServices.resetSecondFactor(user.getExternalId());
        
        verify(userDao, never()).merge(any(UserEntity.class));
        assertThat(result.isPresent(), is(true));
    }

    @Test
    public void resetSecondFactor_shouldReturnEmptyOptional_ifUserNotFound() {
        String externalId = "not-found";
        when(userDao.findByExternalId(externalId)).thenReturn(Optional.empty());

        Optional<User> result = userServices.resetSecondFactor(externalId);

        verify(userDao, never()).merge(any(UserEntity.class));
        assertThat(result.isPresent(), is(false));
    }

    private User aUser() {
        return User.from(randomInt(), USER_EXTERNAL_ID, USER_USERNAME, "random-password",
                "email@example.com","784rh", "8948924", emptyList(),
                null, SecondFactorMethod.SMS,null, null, null);
    }

    private User anotherUser() {
        return User.from(randomInt(), ANOTHER_USER_EXTERNAL_ID, ANOTHER_USER_USERNAME, "random-password",
                "email@example.com", "784rh", "8948924", emptyList(),
                null, SecondFactorMethod.SMS, null, null, null);
    }

    private Role aRole() {
        return role(randomInt(), "role-name-" + randomUuid(), "role-description" + randomUuid());
    }

    private Permission aPermission() {
        return permission(randomInt(), "permission-name-" + randomUuid(), "permission-description" + randomUuid());
    }

    private UserEntity aUserEntityWithTrimmings(User user) {
        UserEntity userEntity = UserEntity.from(user);

        ServiceEntity serviceEntity = new ServiceEntity(List.of("a-gateway-account"));
        serviceEntity.addOrUpdateServiceName(ServiceNameEntity.from(SupportedLanguage.ENGLISH, Service.DEFAULT_NAME_VALUE));
        serviceEntity.setId(randomInt());

        Role role = aRole();
        role.setPermissions(Collections.singletonList(aPermission()));

        ServiceRoleEntity serviceRoleEntity = new ServiceRoleEntity(serviceEntity, new RoleEntity(role));
        userEntity.addServiceRole(serviceRoleEntity);

        return userEntity;
    }
}
