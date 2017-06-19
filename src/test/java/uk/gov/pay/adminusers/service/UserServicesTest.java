package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.pay.adminusers.model.*;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.*;

import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Arrays.asList;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.within;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.Permission.permission;
import static uk.gov.pay.adminusers.model.Role.role;
import static uk.gov.pay.adminusers.resources.UserResource.USERS_RESOURCE;

@RunWith(MockitoJUnitRunner.class)
public class UserServicesTest {

    @Mock
    private UserDao userDao;
    @Mock
    private RoleDao roleDao;
    @Mock
    private ServiceDao serviceDao;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SecondFactorAuthenticator secondFactorAuthenticator;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private UserServices userServices;

    private static final String USER_EXTERNAL_ID = "7d19aff33f8948deb97ed16b2912dcd3";
    private static final String USER_USERNAME = "random-name";

    @Before
    public void before() throws Exception {
        userServices = new UserServices(userDao, roleDao, serviceDao, passwordHasher,
                new LinksBuilder("http://localhost"), 3,
                () -> notificationService, secondFactorAuthenticator);
    }

    @Test(expected = WebApplicationException.class)
    public void shouldError_ifRoleNameDoesNotExist() throws Exception {
        CreateUserRequest createUserRequest = aCreateUserRequest();
        String nonExistentRole = "nonExistentRole";
        when(roleDao.findByRoleName(nonExistentRole)).thenReturn(Optional.empty());
        userServices.createUser(createUserRequest, nonExistentRole);
    }

    @Test
    public void shouldPersistAUser_creatingANewServiceForTheUsersGatewayAccount_whenPersistingTheUserWithNoServiceRelatedToTheGivenGateway() throws Exception {
        CreateUserRequest createUserRequest = aCreateUserRequest();
        Role role = Role.role(2, "admin", "admin role");
        ArgumentCaptor<UserEntity> expectedUser = ArgumentCaptor.forClass(UserEntity.class);
        ArgumentCaptor<ServiceEntity> expectedService = ArgumentCaptor.forClass(ServiceEntity.class);

        when(roleDao.findByRoleName(role.getName())).thenReturn(Optional.of(new RoleEntity(role)));
        when(passwordHasher.hash("random-password")).thenReturn("the hashed random-password");
        when(serviceDao.findByGatewayAccountId(createUserRequest.getGatewayAccountIds().get(0))).thenReturn(Optional.empty());

        doNothing().when(serviceDao).persist(any(ServiceEntity.class));
        doNothing().when(userDao).persist(any(UserEntity.class));

        User persistedUser = userServices.createUser(createUserRequest, role.getName());
        Link selfLink = Link.from(Link.Rel.self, "GET", "http://localhost" + USERS_RESOURCE + "/" + persistedUser.getExternalId());

        assertThat(persistedUser.getUsername(), is(createUserRequest.getUsername()));
        assertThat(persistedUser.getPassword(), is(not(createUserRequest.getPassword())));
        assertThat(persistedUser.getEmail(), is(createUserRequest.getEmail()));
        assertThat(persistedUser.getGatewayAccountIds().size(), is(1));
        assertThat(persistedUser.getGatewayAccountIds().get(0), is("1"));
        assertThat(persistedUser.getTelephoneNumber(), is(createUserRequest.getTelephoneNumber()));
        assertThat(persistedUser.getOtpKey(), is(createUserRequest.getOtpKey()));
        assertThat(persistedUser.getRoles().size(), is(1));
        assertThat(persistedUser.getRoles().get(0), is(role));
        assertThat(persistedUser.getLinks().get(0), is(selfLink));

        verify(serviceDao).persist(expectedService.capture());
        verify(userDao).persist(expectedUser.capture());

        UserEntity savedUser = expectedUser.getValue();
        assertThat(savedUser.getGatewayAccountId(), is(createUserRequest.getGatewayAccountIds().get(0)));

        assertThat(expectedService.getValue().getGatewayAccountId().getGatewayAccountId(), is(createUserRequest.getGatewayAccountIds().get(0)));
    }

    @Test
    public void shouldPersist_aUserSuccessfully_andAssociateToTheServiceRelatedToExistingGatewayAccount() throws Exception {
        CreateUserRequest createUserRequest = aCreateUserRequest();
        Role role = Role.role(2, "admin", "admin role");
        ServiceEntity serviceEntity = new ServiceEntity(newArrayList(createUserRequest.getGatewayAccountIds().get(0)));

        ArgumentCaptor<UserEntity> expectedUser = ArgumentCaptor.forClass(UserEntity.class);

        when(roleDao.findByRoleName(role.getName())).thenReturn(Optional.of(new RoleEntity(role)));
        when(passwordHasher.hash("random-password")).thenReturn("the hashed random-password");
        when(serviceDao.findByGatewayAccountId(createUserRequest.getGatewayAccountIds().get(0)))
                .thenReturn(Optional.of(serviceEntity));
        doNothing().when(userDao).persist(any(UserEntity.class));

        User persistedUser = userServices.createUser(createUserRequest, role.getName());
        Link selfLink = Link.from(Link.Rel.self, "GET", "http://localhost" + USERS_RESOURCE + "/" + persistedUser.getExternalId());

        assertThat(persistedUser.getUsername(), is(createUserRequest.getUsername()));
        assertThat(persistedUser.getPassword(), is(not(createUserRequest.getPassword())));
        assertThat(persistedUser.getEmail(), is(createUserRequest.getEmail()));
        assertThat(persistedUser.getGatewayAccountIds().size(), is(1));
        assertThat(persistedUser.getGatewayAccountIds().get(0), is("1"));
        assertThat(persistedUser.getTelephoneNumber(), is(createUserRequest.getTelephoneNumber()));
        assertThat(persistedUser.getOtpKey(), is(createUserRequest.getOtpKey()));
        assertThat(persistedUser.getRoles().size(), is(1));
        assertThat(persistedUser.getRoles().get(0), is(role));
        assertThat(persistedUser.getLinks().get(0), is(selfLink));

        verify(serviceDao).findByGatewayAccountId(createUserRequest.getGatewayAccountIds().get(0));
        verifyNoMoreInteractions(serviceDao);
        verify(userDao).persist(expectedUser.capture());

        assertThat(expectedUser.getValue().toUser().getGatewayAccountIds().get(0), is(createUserRequest.getGatewayAccountIds().get(0)));
    }

    @Test
    public void shouldFindAUserByExternalId() throws Exception {
        User user = aUser();

        UserEntity userEntity = aUserEntityWithTrimmings(user);

        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(userDao.findByExternalId(USER_EXTERNAL_ID)).thenReturn(userEntityOptional);

        Optional<User> userOptional = userServices.findUserByExternalId(USER_EXTERNAL_ID);
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getExternalId(), is(USER_EXTERNAL_ID));
    }

    @Test
    public void shouldReturnEmpty_WhenFindByExternalId_ifNotFound() throws Exception {
        when(userDao.findByExternalId(USER_EXTERNAL_ID)).thenReturn(Optional.empty());

        Optional<User> userOptional = userServices.findUserByExternalId(USER_EXTERNAL_ID);
        assertFalse(userOptional.isPresent());
    }

    @Test
    public void shouldFindAUserByUserName() throws Exception {
        User user = aUser();

        UserEntity userEntity = aUserEntityWithTrimmings(user);

        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(userDao.findByUsername(USER_USERNAME)).thenReturn(userEntityOptional);

        Optional<User> userOptional = userServices.findUserByUsername(USER_USERNAME);
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getUsername(), is(USER_USERNAME));
    }

    @Test
    public void shouldReturnEmpty_WhenFindByUserName_ifNotFound() throws Exception {
        when(userDao.findByUsername(USER_USERNAME)).thenReturn(Optional.empty());

        Optional<User> userOptional = userServices.findUserByUsername(USER_USERNAME);
        assertFalse(userOptional.isPresent());
    }

    @Test
    public void shouldReturnUserAndResetLoginCount_ifAuthenticationSuccessfulAndUserNotDisabled() throws Exception {
        User user = aUser();
        user.setLoginCounter(2);

        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setPassword("hashed-password");

        when(passwordHasher.isEqual("random-password", "hashed-password")).thenReturn(true);
        when(userDao.findByUsername(USER_USERNAME)).thenReturn(Optional.of(userEntity));
        ArgumentCaptor<UserEntity> argumentCaptor = ArgumentCaptor.forClass(UserEntity.class);
        when(userDao.merge(argumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> userOptional = userServices.authenticate(USER_USERNAME, "random-password");
        assertTrue(userOptional.isPresent());

        User authenticatedUser = userOptional.get();
        assertThat(authenticatedUser.getUsername(), is(USER_USERNAME));
        assertThat(authenticatedUser.getLinks().size(), is(1));
        assertThat(argumentCaptor.getValue().getLoginCounter(), is(0));
    }

    @Test
    public void shouldReturnUserAndNotResetLoginCount_ifAuthenticationSuccessfulButUserDisabled() throws Exception {
        User user = aUser();
        user.setLoginCounter(2);
        user.setDisabled(true);

        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setPassword("hashed-password");

        when(passwordHasher.isEqual("random-password", "hashed-password")).thenReturn(true);
        when(userDao.findByUsername(USER_USERNAME)).thenReturn(Optional.of(userEntity));
        ArgumentCaptor<UserEntity> argumentCaptor = ArgumentCaptor.forClass(UserEntity.class);

        Optional<User> userOptional = userServices.authenticate(USER_USERNAME, "random-password");
        assertTrue(userOptional.isPresent());

        User authenticatedUser = userOptional.get();
        assertThat(authenticatedUser.getUsername(), is(USER_USERNAME));
        assertThat(authenticatedUser.isDisabled(), is(true));
        assertThat(authenticatedUser.getLinks().size(), is(1));
        assertThat(userEntity.getLoginCounter(), is(2));
    }

    @Test
    public void shouldReturnEmptyAndIncrementLoginCount_ifAuthenticationFail() throws Exception {
        User user = aUser();
        user.setLoginCounter(1);
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setPassword("hashed-password");

        when(passwordHasher.isEqual("random-password", "hashed-password")).thenReturn(false);
        when(userDao.findByUsername(USER_USERNAME)).thenReturn(Optional.of(UserEntity.from(user)));
        ArgumentCaptor<UserEntity> argumentCaptor = ArgumentCaptor.forClass(UserEntity.class);
        when(userDao.merge(argumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> userOptional = userServices.authenticate(USER_USERNAME, "random-password");
        assertFalse(userOptional.isPresent());

        UserEntity savedUser = argumentCaptor.getValue();
        assertTrue(within(3, SECONDS, savedUser.getCreatedAt()).matches(savedUser.getUpdatedAt()));
        assertThat(savedUser.getLoginCounter(), is(2));
        assertThat(savedUser.isDisabled(), is(false));
    }

    @Test
    public void shouldLockUser_onTooManyAuthFailures() throws Exception {
        User user = aUser();
        user.setLoginCounter(2);
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        userEntity.setPassword("hashed-password");

        when(passwordHasher.isEqual("random-password", "hashed-password")).thenReturn(false);
        when(userDao.findByUsername(USER_USERNAME)).thenReturn(Optional.of(UserEntity.from(user)));
        ArgumentCaptor<UserEntity> argumentCaptor = ArgumentCaptor.forClass(UserEntity.class);
        when(userDao.merge(argumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        userServices.authenticate(USER_USERNAME, "random-password");
        UserEntity savedUser = argumentCaptor.getValue();
        assertTrue(within(3, SECONDS, savedUser.getCreatedAt()).matches(savedUser.getUpdatedAt()));
        assertThat(savedUser.getLoginCounter(), is(3));
        assertThat(savedUser.isDisabled(), is(true));
    }

    @Test
    public void shouldReturnUser_whenIncrementingSessionVersion_ifUserFound() throws Exception {
        User user = aUser();

        JsonNode node = new ObjectMapper().valueToTree(ImmutableMap.of("path", "sessionVersion", "op", "append", "value", "2"));
        UserEntity userEntity = aUserEntityWithTrimmings(user);

        Optional<UserEntity> userEntityOptional = Optional.of(userEntity);
        when(userDao.findByExternalId(USER_EXTERNAL_ID)).thenReturn(userEntityOptional);

        Optional<User> userOptional = userServices.patchUser(USER_EXTERNAL_ID, PatchRequest.from(node));
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getExternalId(), is(USER_EXTERNAL_ID));
        assertThat(userOptional.get().getSessionVersion(), is(2));
    }

    @Test
    public void shouldReturnUser_withDisabled_ifUserFoundDuringPatch() throws Exception {
        User user = aUser();

        JsonNode node = new ObjectMapper().valueToTree(ImmutableMap.of("path", "disabled", "op", "replace", "value", "true"));

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
    public void shouldResetLoginCounter_whenTheUserIsEnabled() throws Exception {
        User user = aUser();
        user.setDisabled(Boolean.TRUE);
        user.setLoginCounter(11);

        JsonNode node = new ObjectMapper().valueToTree(ImmutableMap.of("path", "disabled", "op", "replace", "value", "false"));
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
    public void shouldReturn2FAToken_whenCreate2FA_ifUserFound() throws Exception {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.newPassCode(user.getOtpKey())).thenReturn(123456);
        CompletableFuture<String> notifyPromise = CompletableFuture.completedFuture("random-notify-id");
        when(notificationService.sendSecondFactorPasscodeSms(any(String.class), eq("123456")))
                .thenReturn(notifyPromise);

        Optional<SecondFactorToken> tokenOptional = userServices.newSecondFactorPasscode(user.getExternalId());

        assertTrue(tokenOptional.isPresent());
        assertThat(tokenOptional.get().getPasscode(), is("123456"));
        assertTrue(notifyPromise.isDone());
    }

    @Test
    public void shouldZeroPad2FATokenTo6Digits_whenCreate2FA() throws Exception {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.newPassCode(user.getOtpKey())).thenReturn(12345);
        CompletableFuture<String> notifyPromise = CompletableFuture.completedFuture("random-notify-id");
        when(notificationService.sendSecondFactorPasscodeSms(any(String.class), eq("012345")))
                .thenReturn(notifyPromise);

        Optional<SecondFactorToken> tokenOptional = userServices.newSecondFactorPasscode(user.getExternalId());

        assertTrue(tokenOptional.isPresent());
        assertThat(tokenOptional.get().getPasscode(), is("012345"));
        assertTrue(notifyPromise.isDone());
    }

    @Test
    public void shouldReturn2FAToken_whenCreate2FA_evenIfNotifyThrowsAnError() throws Exception {
        User user = aUser();
        UserEntity userEntity = UserEntity.from(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.newPassCode(user.getOtpKey())).thenReturn(123456);
        CompletableFuture<String> errorPromise = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("some error from notify");
        });

        when(notificationService.sendSecondFactorPasscodeSms(any(String.class), eq("123456")))
                .thenReturn(errorPromise);

        Optional<SecondFactorToken> tokenOptional = userServices.newSecondFactorPasscode(user.getExternalId());

        assertTrue(tokenOptional.isPresent());
        assertThat(tokenOptional.get().getPasscode(), is("123456"));

        assertTrue(errorPromise.isCompletedExceptionally());
    }

    @Test
    public void shouldReturnEmpty_whenCreate2FA_ifUserNotFound() throws Exception {
        String nonExistentExternalId = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        when(userDao.findByExternalId(nonExistentExternalId)).thenReturn(Optional.empty());

        Optional<SecondFactorToken> tokenOptional = userServices.newSecondFactorPasscode(nonExistentExternalId);

        assertFalse(tokenOptional.isPresent());
    }

    @Test
    public void shouldReturnUser_whenAuthenticate2FA_ifSuccessful() throws Exception {
        User user = aUser();
        int newPassCode = 123456;
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.authorize(user.getOtpKey(), newPassCode)).thenReturn(true);

        Optional<User> tokenOptional = userServices.authenticateSecondFactor(user.getExternalId(), newPassCode);

        assertTrue(tokenOptional.isPresent());
        assertThat(tokenOptional.get().getExternalId(), is(user.getExternalId()));
        assertThat(tokenOptional.get().getLoginCounter(), is(0));
    }

    @Test
    public void shouldReturnEmpty_whenAuthenticate2FA_ifUnsuccessful() throws Exception {
        User user = aUser();
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.authorize(user.getOtpKey(), 123456)).thenReturn(false);
        ArgumentCaptor<UserEntity> argumentCaptor = ArgumentCaptor.forClass(UserEntity.class);
        when(userDao.merge(argumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> tokenOptional = userServices.authenticateSecondFactor(user.getExternalId(), 123456);

        assertFalse(tokenOptional.isPresent());

        UserEntity savedUser = argumentCaptor.getValue();
        assertThat(savedUser.getLoginCounter(), is(1));
        assertThat(savedUser.isDisabled(), is(false));
    }


    @Test
    public void shouldReturnEmptyAndDisable_whenAuthenticate2FA_ifUnsuccessfulMaxRetry() throws Exception {
        User user = aUser();
        user.setLoginCounter(3);
        UserEntity userEntity = aUserEntityWithTrimmings(user);
        when(userDao.findByExternalId(user.getExternalId())).thenReturn(Optional.of(userEntity));
        when(secondFactorAuthenticator.authorize(user.getOtpKey(), 123456)).thenReturn(false);
        ArgumentCaptor<UserEntity> argumentCaptor = ArgumentCaptor.forClass(UserEntity.class);
        when(userDao.merge(argumentCaptor.capture())).thenReturn(mock(UserEntity.class));

        Optional<User> tokenOptional = userServices.authenticateSecondFactor(user.getExternalId(), 123456);

        assertFalse(tokenOptional.isPresent());

        UserEntity savedUser = argumentCaptor.getValue();
        assertThat(savedUser.getLoginCounter(), is(4));
        assertThat(savedUser.isDisabled(), is(true));
    }

    @Test
    public void shouldReturnEmpty_whenAuthenticate2FA_ifUserNotFound() throws Exception {
        String nonExistentExternalId = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        when(userDao.findByExternalId(nonExistentExternalId)).thenReturn(Optional.empty());

        Optional<User> tokenOptional = userServices.authenticateSecondFactor(nonExistentExternalId, 111111);

        assertFalse(tokenOptional.isPresent());
    }

    @Test
    public void createUser_shouldError_whenAddingAUserToDifferentGatewayAccountsBelongingToDifferentServices() {
        ArrayList<String> gatewayAccountIds = newArrayList("1", "2", "3");
        CreateUserRequest createUserRequest = CreateUserRequest.from(USER_USERNAME, "random-password", "email@example.com", gatewayAccountIds, newArrayList(), "784rh", "8948924");
        String roleName = "admin";
        Role role = Role.role(2, roleName, "admin role");
        ArgumentCaptor<ServiceEntity> expectedService = ArgumentCaptor.forClass(ServiceEntity.class);

        when(roleDao.findByRoleName(roleName)).thenReturn(Optional.of(new RoleEntity(role)));
        when(passwordHasher.hash("random-password")).thenReturn("the hashed random-password");

        ServiceEntity service1 = mock(ServiceEntity.class);
        when(service1.getGatewayAccountIds()).thenReturn(newArrayList(new GatewayAccountIdEntity("1", service1), new GatewayAccountIdEntity("6", service1)));
        when(serviceDao.findByGatewayAccountId("1")).thenReturn(Optional.of(service1));

        expectedException.expect(WebApplicationException.class);
        expectedException.expectMessage("409 Conflict");

        userServices.createUser(createUserRequest, roleName);

        verify(serviceDao).findByGatewayAccountId("1");
        verifyNoMoreInteractions(serviceDao);
        verifyZeroInteractions(userDao);
    }

    @Test
    public void createUser_shouldError_whenListOfGatewayAccountsContainsSomeNotBelongingToAnything() {
        ArrayList<String> gatewayAccountIds = newArrayList("1", "2", "3");

        CreateUserRequest createUserRequest = CreateUserRequest.from(USER_USERNAME, "random-password", "email@example.com", gatewayAccountIds, newArrayList(), "784rh", "8948924");
        String roleName = "admin";
        Role role = Role.role(2, roleName, "admin role");
        ArgumentCaptor<ServiceEntity> expectedService = ArgumentCaptor.forClass(ServiceEntity.class);

        when(roleDao.findByRoleName(roleName)).thenReturn(Optional.of(new RoleEntity(role)));
        when(passwordHasher.hash("random-password")).thenReturn("the hashed random-password");

        when(serviceDao.findByGatewayAccountId("1")).thenReturn(Optional.empty());
        ServiceEntity service2 = mock(ServiceEntity.class);
        when(service2.getGatewayAccountIds()).thenReturn(newArrayList(new GatewayAccountIdEntity("2", service2), new GatewayAccountIdEntity("3", service2)));
        when(serviceDao.findByGatewayAccountId("2")).thenReturn(Optional.of(service2));

        expectedException.expect(WebApplicationException.class);
        expectedException.expectMessage("409 Conflict");

        userServices.createUser(createUserRequest, roleName);

        verify(serviceDao).findByGatewayAccountId("1");
        verify(serviceDao).findByGatewayAccountId("2");
        verifyNoMoreInteractions(serviceDao);
        verifyZeroInteractions(userDao);
    }

    @Test
    public void createUser_shouldPersist_aUserSuccessfully_andCreateANewServiceAssociatingAllNonExistingGatewayAccounts() throws Exception {
        ArrayList<String> gatewayAccountIds = newArrayList("1", "2", "3");

        CreateUserRequest createUserRequest = CreateUserRequest.from(USER_USERNAME, "random-password", "email@example.com", gatewayAccountIds, newArrayList(), "784rh", "8948924");
        Role role = Role.role(2, "admin", "admin role");

        ArgumentCaptor<UserEntity> expectedUser = ArgumentCaptor.forClass(UserEntity.class);
        ArgumentCaptor<ServiceEntity> expectedService = ArgumentCaptor.forClass(ServiceEntity.class);

        when(roleDao.findByRoleName(role.getName())).thenReturn(Optional.of(new RoleEntity(role)));
        when(passwordHasher.hash("random-password")).thenReturn("the hashed random-password");
        when(serviceDao.findByGatewayAccountId("1")).thenReturn(Optional.empty());
        when(serviceDao.findByGatewayAccountId("2")).thenReturn(Optional.empty());
        when(serviceDao.findByGatewayAccountId("3")).thenReturn(Optional.empty());

        doNothing().when(userDao).persist(any(UserEntity.class));
        doNothing().when(serviceDao).persist(any(ServiceEntity.class));

        User persistedUser = userServices.createUser(createUserRequest, role.getName());
        Link selfLink = Link.from(Link.Rel.self, "GET", "http://localhost" + USERS_RESOURCE + "/" + persistedUser.getExternalId());

        assertThat(persistedUser.getUsername(), is(createUserRequest.getUsername()));
        assertThat(persistedUser.getPassword(), is(not(createUserRequest.getPassword())));
        assertThat(persistedUser.getEmail(), is(createUserRequest.getEmail()));
        assertThat(persistedUser.getGatewayAccountIds().size(), is(3));
        assertThat(persistedUser.getGatewayAccountIds().get(0), is("1"));
        assertThat(persistedUser.getGatewayAccountIds().get(1), is("2"));
        assertThat(persistedUser.getGatewayAccountIds().get(2), is("3"));
        assertThat(persistedUser.getTelephoneNumber(), is(createUserRequest.getTelephoneNumber()));
        assertThat(persistedUser.getOtpKey(), is(createUserRequest.getOtpKey()));
        assertThat(persistedUser.getRoles().size(), is(1));
        assertThat(persistedUser.getRoles().get(0), is(role));
        assertThat(persistedUser.getLinks().get(0), is(selfLink));

        verify(serviceDao).findByGatewayAccountId("1");
        verify(serviceDao).findByGatewayAccountId("2");
        verify(serviceDao).findByGatewayAccountId("3");
        verify(serviceDao).persist(expectedService.capture());
        verifyNoMoreInteractions(serviceDao);
        verify(userDao).persist(expectedUser.capture());

        assertThat(expectedService.getValue().getGatewayAccountIds().get(0).getGatewayAccountId(), is("1"));
        assertThat(expectedService.getValue().getGatewayAccountIds().get(1).getGatewayAccountId(), is("2"));
        assertThat(expectedService.getValue().getGatewayAccountIds().get(2).getGatewayAccountId(), is("3"));
    }

    private CreateUserRequest aCreateUserRequest() {
        return CreateUserRequest.from(USER_USERNAME, "random-password", "email@example.com", asList("1"), newArrayList(), "784rh", "8948924");
    }

    private User aUser() {
        return User.from(randomInt(), USER_EXTERNAL_ID, USER_USERNAME, "random-password", "email@example.com", asList("1"), newArrayList(), "784rh", "8948924");
    }

    private Role aRole() {
        return role(randomInt(), "role-name-" + randomUuid(), "role-description" + randomUuid());
    }

    private Permission aPermission() {
        return permission(randomInt(), "permission-name-" + randomUuid(), "permission-description" + randomUuid());
    }

    private UserEntity aUserEntityWithTrimmings(User user) {
        UserEntity userEntity = UserEntity.from(user);

        ServiceEntity serviceEntity = new ServiceEntity(newArrayList("a-gateway-account"));
        serviceEntity.setId(randomInt());

        Role role = aRole();
        role.setPermissions(asList(aPermission()));

        ServiceRoleEntity serviceRoleEntity = new ServiceRoleEntity(serviceEntity, new RoleEntity(role));
        userEntity.setServiceRole(serviceRoleEntity);

        return userEntity;
    }
}
