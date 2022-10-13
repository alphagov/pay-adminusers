package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.model.PatchRequest;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceName;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.Permission.permission;
import static uk.gov.pay.adminusers.model.Role.role;
import static uk.gov.pay.adminusers.model.Service.DEFAULT_NAME_VALUE;

@ExtendWith(MockitoExtension.class)
public class UserServicesTest {

    @Mock
    private UserDao mockUserDao;
    @Mock
    private PasswordHasher mockPasswordHasher;
    @Mock
    private NotificationService mockNotificationService;
    @Mock
    private SecondFactorAuthenticator mockSecondFactorAuthenticator;
    @Captor
    private ArgumentCaptor<UserEntity> userEntityArgumentCaptor;

    private UserServices underTest;

    private static final String USER_EXTERNAL_ID = "7d19aff33f8948deb97ed16b2912dcd3";
    private static final String USER_USERNAME = "random-name";
    private static final String ANOTHER_USER_EXTERNAL_ID = "7d19aff33f8948deb97ed16b2912dcd4";
    private static final String ANOTHER_USER_USERNAME = "another-random-name";

    private static ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void before() {
        underTest = new UserServices(mockUserDao, mockPasswordHasher, new LinksBuilder("http://localhost"), 3, () -> mockNotificationService, mockSecondFactorAuthenticator, mock(ServiceFinder.class));
    }

    @Test
    void shouldFindAUserByExternalId() {
        User user = aUser();

        UserEntity userEntity = aUserEntityWithTrimmings(user);

        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(mockUserDao.findByExternalId(USER_EXTERNAL_ID)).thenReturn(userEntityOptional);

        Optional<UserEntity> userOptional = underTest.findUserByExternalId(USER_EXTERNAL_ID);
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getExternalId(), is(USER_EXTERNAL_ID));
    }

    @Test
    void shouldFindAUsersByExternalIds() {
        User user1 = aUser();
        User user2 = anotherUser();

        UserEntity userEntity1 = aUserEntityWithTrimmings(user1);
        UserEntity userEntity2 = aUserEntityWithTrimmings(user2);

        when(mockUserDao.findByExternalIds(List.of(user1.getExternalId(), user2.getExternalId()))).thenReturn(Arrays.asList(userEntity1, userEntity2));

        List<User> users = underTest.findUsersByExternalIds(List.of(user1.getExternalId(), user2.getExternalId()));
        assertThat(users.size(), is(2));

        assertThat(users.get(0).getExternalId(), is(user1.getExternalId()));
        assertThat(users.get(1).getExternalId(), is(user2.getExternalId()));
    }

    @Test
    void shouldReturnEmpty_WhenFindByExternalId_ifNotFound() {
        when(mockUserDao.findByExternalId(USER_EXTERNAL_ID)).thenReturn(Optional.empty());

        Optional<UserEntity> userOptional = underTest.findUserByExternalId(USER_EXTERNAL_ID);
        assertFalse(userOptional.isPresent());
    }

    @Test
    void shouldFindAUserByUserName() {
        User user = aUser();

        UserEntity userEntity = aUserEntityWithTrimmings(user);

        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(mockUserDao.findByUsername(USER_USERNAME)).thenReturn(userEntityOptional);

        Optional<User> userOptional = underTest.findUserByUsername(USER_USERNAME);
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getUsername(), is(USER_USERNAME));
    }

    @Test
    void shouldReturnEmpty_WhenFindByUserName_ifNotFound() {
        when(mockUserDao.findByUsername(USER_USERNAME)).thenReturn(Optional.empty());

        Optional<User> userOptional = underTest.findUserByUsername(USER_USERNAME);
        assertFalse(userOptional.isPresent());
    }

    @Test
    void shouldReturnUserAndResetLoginCount_ifAuthenticationSuccessfulAndUserNotDisabled() {
        User user = aUser();
        user.setLoginCounter(2);

        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setPassword("hashed-password");

        when(mockPasswordHasher.isEqual("random-password", "hashed-password")).thenReturn(true);
        when(mockUserDao.findByUsername(USER_USERNAME)).thenReturn(Optional.of(userEntity));
        when(mockUserDao.merge(userEntityArgumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> userOptional = underTest.authenticate(USER_USERNAME, "random-password");
        assertTrue(userOptional.isPresent());

        User authenticatedUser = userOptional.get();
        assertThat(authenticatedUser.getUsername(), is(USER_USERNAME));
        assertThat(authenticatedUser.getLinks().size(), is(1));
        assertThat(userEntityArgumentCaptor.getValue().getLoginCounter(), is(0));
    }

    @Test
    void shouldReturnUserAndNotResetLoginCount_ifAuthenticationSuccessfulButUserDisabled() {
        User user = aUser();
        user.setLoginCounter(2);
        user.setDisabled(true);

        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setPassword("hashed-password");

        when(mockPasswordHasher.isEqual("random-password", "hashed-password")).thenReturn(true);
        when(mockUserDao.findByUsername(USER_USERNAME)).thenReturn(Optional.of(userEntity));

        Optional<User> userOptional = underTest.authenticate(USER_USERNAME, "random-password");
        assertTrue(userOptional.isPresent());

        User authenticatedUser = userOptional.get();
        assertThat(authenticatedUser.getUsername(), is(USER_USERNAME));
        assertThat(authenticatedUser.isDisabled(), is(true));
        assertThat(authenticatedUser.getLinks().size(), is(1));
        assertThat(userEntity.getLoginCounter(), is(2));
    }

    @Test
    void shouldReturnEmptyAndIncrementLoginCount_ifAuthenticationFail() {
        User user = aUser();
        user.setLoginCounter(1);
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setPassword("hashed-password");

        when(mockUserDao.findByUsername(USER_USERNAME)).thenReturn(Optional.of(UserEntity.from(user)));
        when(mockUserDao.merge(userEntityArgumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> userOptional = underTest.authenticate(USER_USERNAME, "random-password");
        assertFalse(userOptional.isPresent());

        UserEntity savedUser = userEntityArgumentCaptor.getValue();
        assertTrue(within(3, SECONDS, savedUser.getCreatedAt()).matches(savedUser.getUpdatedAt()));
        assertThat(savedUser.getLoginCounter(), is(2));
        assertThat(savedUser.isDisabled(), is(false));
    }

    @Test
    void shouldLockUser_onTooManyAuthFailures() {
        User user = aUser();
        user.setLoginCounter(2);
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setPassword("hashed-password");

        when(mockUserDao.findByUsername(USER_USERNAME)).thenReturn(Optional.of(UserEntity.from(user)));
        when(mockUserDao.merge(userEntityArgumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        underTest.authenticate(USER_USERNAME, "random-password");
        UserEntity savedUser = userEntityArgumentCaptor.getValue();
        assertTrue(within(3, SECONDS, savedUser.getCreatedAt()).matches(savedUser.getUpdatedAt()));
        assertThat(savedUser.getLoginCounter(), is(3));
        assertThat(savedUser.isDisabled(), is(true));
    }

    @Test
    void shouldReturnUser_whenIncrementingSessionVersion_ifUserFound() {
        User user = aUser();

        JsonNode node = objectMapper.valueToTree(Map.of("path", "sessionVersion", "op", "append", "value", "2"));
        UserEntity userEntity = aUserEntityWithTrimmings(user);

        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(mockUserDao.findByExternalId(USER_EXTERNAL_ID)).thenReturn(userEntityOptional);

        Optional<User> userOptional = underTest.patchUser(USER_EXTERNAL_ID, PatchRequest.from(node));
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getExternalId(), is(USER_EXTERNAL_ID));
        assertThat(userOptional.get().getSessionVersion(), is(2));
    }

    @Test
    void shouldReturnUser_withDisabled_ifUserFoundDuringPatch() {
        User user = aUser();

        JsonNode node = objectMapper.valueToTree(Map.of("path", "disabled", "op", "replace", "value", "true"));

        UserEntity userEntity = aUserEntityWithTrimmings(user);

        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(mockUserDao.findByExternalId(USER_EXTERNAL_ID)).thenReturn(userEntityOptional);

        assertFalse(user.isDisabled());

        Optional<User> userOptional = underTest.patchUser(USER_EXTERNAL_ID, PatchRequest.from(node));
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getExternalId(), is(USER_EXTERNAL_ID));
        assertTrue(userOptional.get().isDisabled());
    }

    @Test
    void shouldResetLoginCounter_whenTheUserIsEnabled() {
        User user = aUser();
        user.setDisabled(Boolean.TRUE);
        user.setLoginCounter(11);

        JsonNode node = objectMapper.valueToTree(Map.of("path", "disabled", "op", "replace", "value", "false"));
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(mockUserDao.findByExternalId(USER_EXTERNAL_ID)).thenReturn(userEntityOptional);

        assertTrue(user.isDisabled());
        assertThat(user.getLoginCounter(), is(11));

        Optional<User> userOptional = underTest.patchUser(USER_EXTERNAL_ID, PatchRequest.from(node));
        assertTrue(userOptional.isPresent());

        assertFalse(userOptional.get().isDisabled());
        assertThat(userOptional.get().getLoginCounter(), is(0));
    }

    @Test
    void shouldUpdateTelephoneNumber_whenReplacingTelephoneNumber_ifUserFound() {
        User user = aUser();
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setTelephoneNumber("+447700900000");

        String newTelephoneNumber = "+441134960000";
        JsonNode node = objectMapper.valueToTree(Map.of("path", "telephone_number", "op", "replace", "value", newTelephoneNumber));
        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);

        when(mockUserDao.findByExternalId(USER_EXTERNAL_ID)).thenReturn(userEntityOptional);

        Optional<User> userOptional = underTest.patchUser(USER_EXTERNAL_ID, PatchRequest.from(node));

        verify(mockUserDao, times(1)).merge(userEntityArgumentCaptor.capture());

        UserEntity persistedUser = userEntityArgumentCaptor.getValue();
        assertThat(persistedUser.getTelephoneNumber().get(), is(newTelephoneNumber));
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getTelephoneNumber(), is(newTelephoneNumber));
    }

    @Test
    void shouldUpdateFeatures_whenPathIsFeatures_ifUserFound() {
        User user = aUser();
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setFeatures("1");

        String newFeature = "1,2,3";
        JsonNode node = objectMapper.valueToTree(Map.of("path", "features", "op", "replace", "value", newFeature));
        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);

        when(mockUserDao.findByExternalId(USER_EXTERNAL_ID)).thenReturn(userEntityOptional);

        Optional<User> userOptional = underTest.patchUser(USER_EXTERNAL_ID, PatchRequest.from(node));

        verify(mockUserDao, times(1)).merge(userEntityArgumentCaptor.capture());

        UserEntity persistedUser = userEntityArgumentCaptor.getValue();
        assertThat(persistedUser.getFeatures(), is(newFeature));
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getFeatures(), is(newFeature));
    }

    @Test
    void shouldReturnUser_whenAuthenticate2FA_ifSuccessful() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        User aUser = aUser();
        int newPassCode = 123456;
        UserEntity userEntity = aUserEntityWithTrimmings(aUser);
        userEntity.setLastLoggedInAt(now);
        when(mockUserDao.findByExternalId(aUser.getExternalId())).thenReturn(Optional.of(userEntity));
        when(mockSecondFactorAuthenticator.authorize(aUser.getOtpKey(), newPassCode)).thenReturn(true);

        Optional<User> userOptional = underTest.authenticateSecondFactor(aUser.getExternalId(), newPassCode);

        assertTrue(userOptional.isPresent());
        User user = userOptional.get();
        assertThat(user.getExternalId(), is(aUser.getExternalId()));
        assertThat(user.getLoginCounter(), is(0));
        assertThat(user.getLastLoggedInAt().isAfter(ZonedDateTime.now().minusSeconds(10)), is(true));
    }

    @Test
    void shouldReturnEmpty_whenAuthenticate2FA_ifUnsuccessful_whenTheUserNeverLoggedIn() {
        User user = aUser();
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        when(mockUserDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(mockSecondFactorAuthenticator.authorize(user.getOtpKey(), 123456)).thenReturn(false);
        when(mockUserDao.merge(userEntityArgumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> tokenOptional = underTest.authenticateSecondFactor(user.getExternalId(), 123456);

        assertFalse(tokenOptional.isPresent());

        UserEntity savedUser = userEntityArgumentCaptor.getValue();
        assertThat(savedUser.getLoginCounter(), is(1));
        assertThat(savedUser.isDisabled(), is(false));
        assertThat(savedUser.getLastLoggedInAt(), is(nullValue()));
    }

    @Test
    void shouldReturnEmpty_whenAuthenticate2FA_ifUnsuccessful_whenTheUserLoggedInAtLeastOnce() {
        ZonedDateTime lastLoggedInDateTime = ZonedDateTime.now(ZoneId.of("UTC")).minusDays(7);
        User user = aUser();
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setLastLoggedInAt(lastLoggedInDateTime);
        when(mockUserDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(mockSecondFactorAuthenticator.authorize(user.getOtpKey(), 123456)).thenReturn(false);
        when(mockUserDao.merge(userEntityArgumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> tokenOptional = underTest.authenticateSecondFactor(user.getExternalId(), 123456);

        assertFalse(tokenOptional.isPresent());

        UserEntity savedUser = userEntityArgumentCaptor.getValue();
        assertThat(savedUser.getLoginCounter(), is(1));
        assertThat(savedUser.isDisabled(), is(false));
        assertThat(savedUser.getLastLoggedInAt().equals(lastLoggedInDateTime), is(true));
    }

    @Test
    void shouldReturnEmptyAndDisable_whenAuthenticate2FA_ifUnsuccessfulMaxRetry() {
        User user = aUser();
        user.setLoginCounter(3);
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        when(mockUserDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(mockSecondFactorAuthenticator.authorize(user.getOtpKey(), 123456)).thenReturn(false);
        when(mockUserDao.merge(userEntityArgumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> tokenOptional = underTest.authenticateSecondFactor(user.getExternalId(), 123456);

        assertFalse(tokenOptional.isPresent());

        UserEntity savedUser = userEntityArgumentCaptor.getValue();
        assertThat(savedUser.getLoginCounter(), is(4));
        assertThat(savedUser.isDisabled(), is(true));
    }

    @Test
    void shouldReturnEmpty_whenAuthenticate2FA_ifUserNotFound() {
        String nonExistentExternalId = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        when(mockUserDao.findByExternalId(nonExistentExternalId)).thenReturn(Optional.empty());

        Optional<User> tokenOptional = underTest.authenticateSecondFactor(nonExistentExternalId, 111111);

        assertFalse(tokenOptional.isPresent());
    }

    @Test
    void shouldReturnUser_whenProvisionNewOtpKey_ifUserFound() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setOtpKey("Original OTP key");

        when(mockUserDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(mockSecondFactorAuthenticator.generateNewBase32EncodedSecret()).thenReturn("Provisional OTP key");

        Optional<User> result = underTest.provisionNewOtpKey(user.getExternalId());

        assertThat(result.get().getOtpKey(), is("Original OTP key"));
        assertThat(result.get().getProvisionalOtpKey(), is("Provisional OTP key"));
        assertTrue(within(3, SECONDS, result.get().getProvisionalOtpKeyCreatedAt()).matches(ZonedDateTime.now(ZoneOffset.UTC)));

        verify(mockUserDao).merge(userEntityArgumentCaptor.capture());
        UserEntity savedUser = userEntityArgumentCaptor.getValue();
        assertThat(savedUser.getOtpKey(), is("Original OTP key"));
        assertThat(savedUser.getProvisionalOtpKey(), is("Provisional OTP key"));
        assertTrue(within(3, SECONDS, savedUser.getProvisionalOtpKeyCreatedAt()).matches(ZonedDateTime.now(ZoneOffset.UTC)));
    }

    @Test
    void shouldReturnEmpty_whenProvisionNewOtpKey_ifUserNotFound() {
        String nonExistentExternalId = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        when(mockUserDao.findByExternalId(nonExistentExternalId)).thenReturn(Optional.empty());

        Optional<User> result = underTest.provisionNewOtpKey(nonExistentExternalId);

        assertFalse(result.isPresent());

        verify(mockUserDao, never()).merge(any(UserEntity.class));
    }

    @Test
    void shouldReturnEmpty_whenProvisionNewOtpKey_ifUserDisabled() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setOtpKey("Original OTP key");
        userEntity.setDisabled(true);

        when(mockUserDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));

        Optional<User> result = underTest.provisionNewOtpKey(user.getExternalId());

        assertFalse(result.isPresent());

        assertThat(userEntity.getOtpKey(), is("Original OTP key"));
        assertThat(userEntity.getProvisionalOtpKey(), is(nullValue()));
        assertThat(userEntity.getProvisionalOtpKeyCreatedAt(), is(nullValue()));

        verify(mockUserDao, never()).merge(any(UserEntity.class));
    }

    @Test
    void shouldReturnUser_whenActivateNewOtpKey_ifUserFound() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setSecondFactor(SecondFactorMethod.SMS);
        userEntity.setOtpKey("Original OTP key");
        userEntity.setProvisionalOtpKey("New OTP key");
        userEntity.setProvisionalOtpKeyCreatedAt(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(89));

        when(mockUserDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(mockSecondFactorAuthenticator.authorize("New OTP key", 123456)).thenReturn(true);

        Optional<User> result = underTest.activateNewOtpKey(user.getExternalId(), SecondFactorMethod.APP, 123456);

        assertThat(result.get().getOtpKey(), is("New OTP key"));
        assertThat(result.get().getProvisionalOtpKey(), is(nullValue()));
        assertThat(result.get().getProvisionalOtpKeyCreatedAt(), is(nullValue()));

        verify(mockUserDao).merge(userEntityArgumentCaptor.capture());
        UserEntity savedUser = userEntityArgumentCaptor.getValue();
        assertThat(savedUser.getOtpKey(), is("New OTP key"));
        assertThat(savedUser.getProvisionalOtpKey(), is(nullValue()));
        assertThat(savedUser.getProvisionalOtpKey(), is(nullValue()));
    }

    @Test
    void shouldReturnUser_whenActivateNewOtpKey_ifCodeIncorrect() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setSecondFactor(SecondFactorMethod.SMS);
        userEntity.setOtpKey("Original OTP key");
        userEntity.setProvisionalOtpKey("New OTP key");
        userEntity.setProvisionalOtpKeyCreatedAt(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(89));

        when(mockUserDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(mockSecondFactorAuthenticator.authorize("New OTP key", 123456)).thenReturn(false);

        Optional<User> result = underTest.activateNewOtpKey(user.getExternalId(), SecondFactorMethod.APP, 123456);

        assertFalse(result.isPresent());

        assertThat(userEntity.getOtpKey(), is("Original OTP key"));
        assertThat(userEntity.getSecondFactor(), is(SecondFactorMethod.SMS));

        verify(mockUserDao, never()).merge(any(UserEntity.class));
    }

    @Test
    void shouldReturnUser_whenActivateNewOtpKey_ifNoProvisionalOtpCode() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setSecondFactor(SecondFactorMethod.SMS);
        userEntity.setOtpKey("Original OTP key");
        userEntity.setProvisionalOtpKeyCreatedAt(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(89));

        when(mockUserDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));

        Optional<User> result = underTest.activateNewOtpKey(user.getExternalId(), SecondFactorMethod.APP, 123456);

        assertFalse(result.isPresent());

        assertThat(userEntity.getOtpKey(), is("Original OTP key"));
        assertThat(userEntity.getSecondFactor(), is(SecondFactorMethod.SMS));

        verify(mockUserDao, never()).merge(any(UserEntity.class));
    }

    @Test
    void shouldReturnUser_whenActivateNewOtpKey_ifNoProvisionalOtpCodeCreatedAt() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setSecondFactor(SecondFactorMethod.SMS);
        userEntity.setOtpKey("Original OTP key");
        userEntity.setProvisionalOtpKey("New OTP key");

        when(mockUserDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));

        Optional<User> result = underTest.activateNewOtpKey(user.getExternalId(), SecondFactorMethod.APP, 123456);

        assertFalse(result.isPresent());

        assertThat(userEntity.getOtpKey(), is("Original OTP key"));
        assertThat(userEntity.getSecondFactor(), is(SecondFactorMethod.SMS));

        verify(mockUserDao, never()).merge(any(UserEntity.class));
    }

    @Test
    void shouldReturnUser_whenActivateNewOtpKey_ifProvisionalOtpCodeCreatedAtTooLongAgo() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setSecondFactor(SecondFactorMethod.SMS);
        userEntity.setOtpKey("Original OTP key");
        userEntity.setProvisionalOtpKey("New OTP key");
        userEntity.setProvisionalOtpKeyCreatedAt(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(91));

        when(mockUserDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));

        Optional<User> result = underTest.activateNewOtpKey(user.getExternalId(), SecondFactorMethod.APP, 123456);

        assertFalse(result.isPresent());

        assertThat(userEntity.getOtpKey(), is("Original OTP key"));
        assertThat(userEntity.getSecondFactor(), is(SecondFactorMethod.SMS));

        verify(mockUserDao, never()).merge(any(UserEntity.class));
    }

    @Test
    void shouldReturnEmpty_whenActivateNewOtpKey_ifUserNotFound() {
        String nonExistentExternalId = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        when(mockUserDao.findByExternalId(nonExistentExternalId)).thenReturn(Optional.empty());

        Optional<User> result = underTest.activateNewOtpKey(nonExistentExternalId, SecondFactorMethod.SMS, 123456);

        assertFalse(result.isPresent());

        verify(mockUserDao, never()).merge(any(UserEntity.class));
    }

    @Test
    void shouldReturnUser_whenActivateNewOtpKey_ifUserDisabled() {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        userEntity.setSecondFactor(SecondFactorMethod.SMS);
        userEntity.setOtpKey("Original OTP key");
        userEntity.setProvisionalOtpKey("New OTP key");
        userEntity.setProvisionalOtpKeyCreatedAt(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(89));
        userEntity.setDisabled(true);

        when(mockUserDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));

        Optional<User> result = underTest.activateNewOtpKey(user.getExternalId(), SecondFactorMethod.APP, 123456);

        assertFalse(result.isPresent());

        assertThat(userEntity.getOtpKey(), is("Original OTP key"));
        assertThat(userEntity.getSecondFactor(), is(SecondFactorMethod.SMS));

        verify(mockUserDao, never()).merge(any(UserEntity.class));
    }

    @Test
    void resetSecondFactor_shouldUpdateOtpMethodAndSetNewOtpKey_ifSecondFactorMethodIsApp() {
        User user = aUser();
        user.setSecondFactor(SecondFactorMethod.APP);
        UserEntity userEntity = UserEntity.from(user);

        String newOtpKey = "newOtpKey";
        when(mockUserDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(mockSecondFactorAuthenticator.generateNewBase32EncodedSecret()).thenReturn(newOtpKey);

        Optional<User> result = underTest.resetSecondFactor(user.getExternalId());

        verify(mockUserDao, times(1)).merge(userEntityArgumentCaptor.capture());

        UserEntity persistedUser = userEntityArgumentCaptor.getValue();

        assertThat(result.isPresent(), is(true));
        assertThat(persistedUser.getOtpKey(), is(newOtpKey));
        assertThat(persistedUser.getSecondFactor(), is(SecondFactorMethod.SMS));
    }

    @Test
    void resetSecondFactor_shouldNotUpdateUser_ifSecondFactorMethodIsSms() {
        User user = aUser();
        user.setSecondFactor(SecondFactorMethod.SMS);
        UserEntity userEntity = UserEntity.from(user);
        when(mockUserDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));

        Optional<User> result = underTest.resetSecondFactor(user.getExternalId());

        verify(mockUserDao, never()).merge(any(UserEntity.class));
        assertThat(result.isPresent(), is(true));
    }

    @Test
    void resetSecondFactor_shouldReturnEmptyOptional_ifUserNotFound() {
        String externalId = "not-found";
        when(mockUserDao.findByExternalId(externalId)).thenReturn(Optional.empty());

        Optional<User> result = underTest.resetSecondFactor(externalId);

        verify(mockUserDao, never()).merge(any(UserEntity.class));
        assertThat(result.isPresent(), is(false));
    }

    @Test
    void getAdminUserEmailsForGatewayAccountIdsReturnsEachInputGatewayAccountIdMappedToPossiblyEmptyListOfAdminEmails() {
        when(mockUserDao.getAdminUserEmailsForGatewayAccountIds(List.of("1", "2", "3", "4", "5"))).thenReturn(Map.of("1", List.of("john@beatles.test", "paul@beatles.test"), "3", List.of("george@beatles.test"), "5", List.of("ringo@beatles.test")));

        Map<String, List<String>> result = underTest.getAdminUserEmailsForGatewayAccountIds(List.of("1", "2", "3", "4", "5"));

        assertThat(result.size(), is(5));
        assertThat(result.get("1"), is(List.of("john@beatles.test", "paul@beatles.test")));
        assertThat(result.get("2"), is(List.of()));
        assertThat(result.get("3"), is(List.of("george@beatles.test")));
        assertThat(result.get("4"), is(List.of()));
        assertThat(result.get("5"), is(List.of("ringo@beatles.test")));
    }

    @Test
    void getAdminUsersForServiceShouldReturnListOfUserEntitiesWithAdminPermissions() {
        var serv = Service.from(randomInt(), randomUuid(), new ServiceName(DEFAULT_NAME_VALUE));
        var users = Arrays.asList(
                aUserEntityWithRoleForService(serv, true, "admin1"), 
                aUserEntityWithRoleForService(serv, true, "admin2"), 
                aUserEntityWithRoleForService(serv, false, "user1"));
        when(mockUserDao.findByServiceId(serv.getId())).thenReturn(users);

        var adminUsers = underTest.getAdminUsersForService(serv.getId());

        assertThat(adminUsers.size(), is(2));
        assertThat(adminUsers, hasItems(users.get(0), users.get(1)));
        assertThat(adminUsers, not(hasItem(users.get(2))));
    }

    private User aUser() {
        return User.from(randomInt(), USER_EXTERNAL_ID, USER_USERNAME, "random-password", "email@example.com", "784rh", "8948924", emptyList(), null, SecondFactorMethod.SMS, null, null, null);
    }

    private User anotherUser() {
        return User.from(randomInt(), ANOTHER_USER_EXTERNAL_ID, ANOTHER_USER_USERNAME, "random-password", "email@example.com", "784rh", "8948924", emptyList(), null, SecondFactorMethod.SMS, null, null, null);
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
        role.setPermissions(Set.of(aPermission()));

        ServiceRoleEntity serviceRoleEntity = new ServiceRoleEntity(serviceEntity, new RoleEntity(role));
        userEntity.addServiceRole(serviceRoleEntity);

        return userEntity;
    }

    public static UserEntity aUserEntityWithRoleForService(Service service, boolean isAdmin, String username) {
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(username);
        userEntity.setEmail(format("%s@service.gov.uk", userEntity.getUsername()));
        Role role = role(isAdmin ? 2 : 1, "role", "role-desc");
        role.setPermissions(Set.of(
                permission(1, "perm1", "perm1 desc"), 
                permission(2, "perm2", "perm2 desc")));
        var serviceRoleEntity = new ServiceRoleEntity(ServiceEntity.from(service), new RoleEntity(role));
        userEntity.addServiceRole(serviceRoleEntity);
        return userEntity;
    }
}
