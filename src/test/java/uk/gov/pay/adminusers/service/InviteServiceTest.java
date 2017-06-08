package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.app.config.LinksConfig;
import uk.gov.pay.adminusers.model.*;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.*;

import javax.ws.rs.WebApplicationException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.*;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.Response.Status.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_CODE;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_PASSWORD;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_TELEPHONE_NUMBER;
import static uk.gov.pay.adminusers.model.InviteUserRequest.*;
import static uk.gov.pay.adminusers.model.Role.role;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

@RunWith(MockitoJUnitRunner.class)
public class InviteServiceTest {

    private static final String SELFSERVICE_URL = "http://selfservice";

    @Mock
    private RoleDao mockRoleDao;
    @Mock
    private ServiceDao mockServiceDao;
    @Mock
    private UserDao mockUserDao;
    @Mock
    private InviteDao mockInviteDao;
    @Mock
    private AdminUsersConfig mockConfig;
    @Mock
    private NotificationService mockNotificationService;
    @Mock
    private PasswordHasher mockPasswordHasher;
    @Mock
    private SecondFactorAuthenticator mockSecondFactorAuthenticator;

    private InviteService inviteService;
    private ArgumentCaptor<InviteEntity> expectedInvite = ArgumentCaptor.forClass(InviteEntity.class);
    private ArgumentCaptor<UserEntity> expectedInvitedUser = ArgumentCaptor.forClass(UserEntity.class);
    private int passCode = 123456;
    private String otpKey = "otpKey";
    private String inviteCode = "code";
    private String senderEmail = "sender@example.com";
    private String email = "invited@example.com";
    private int serviceId = 1;
    private String serviceExternalId = "3453rmeuty87t";
    private String senderExternalId = "12345";
    private String roleName = "view-only";

    @Before
    public void setup() {
        LinksConfig mockLinks = mock(LinksConfig.class);
        when(mockLinks.getSelfserviceUrl()).thenReturn(SELFSERVICE_URL);
        when(mockConfig.getLinks()).thenReturn(mockLinks);
        inviteService = new InviteService(
                mockRoleDao,
                mockServiceDao,
                mockUserDao,
                mockInviteDao,
                mockConfig,
                mockPasswordHasher,
                mockNotificationService,
                mockSecondFactorAuthenticator,
                new LinksBuilder("http://localhost"),
                3
        );
    }

    private InviteEntity mocksCreateInvite() {

        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());
        when(mockInviteDao.findByEmail(email)).thenReturn(Optional.empty());
        when(mockServiceDao.findById(serviceId)).thenReturn(Optional.of(service));
        when(mockRoleDao.findByRoleName(roleName)).thenReturn(Optional.of(new RoleEntity()));

        UserEntity senderUser = new UserEntity();
        senderUser.setExternalId(senderExternalId);
        senderUser.setEmail(senderEmail);
        RoleEntity role = new RoleEntity(role(ADMIN.getId(), "admin", "Admin Role"));
        senderUser.setServiceRole(new ServiceRoleEntity(service, role));
        when(mockUserDao.findByExternalId(senderExternalId)).thenReturn(Optional.of(senderUser));

        InviteEntity anInvite = anInvite(email, inviteCode, otpKey, senderUser, service, role);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));

        doNothing().when(mockInviteDao).persist(any(InviteEntity.class));

        return anInvite;
    }

    @Test
    public void create_shouldSendNotificationOnSuccessfulInvite() throws Exception {

        mocksCreateInvite();

        CompletableFuture<String> notifyPromise = CompletableFuture.completedFuture("random-notify-id");

        when(mockNotificationService.sendInviteEmail(eq(senderEmail), eq(email), matches("^http://selfservice/invites/[0-9a-z]{32}$")))
                .thenReturn(notifyPromise);

        inviteService.create(inviteRequestFrom(senderExternalId, email, roleName), serviceId);

        verify(mockInviteDao).persist(expectedInvite.capture());
        InviteEntity savedInvite = expectedInvite.getValue();

        assertThat(savedInvite.getEmail(), is(email));
        assertThat(savedInvite.getOtpKey(), is(notNullValue()));
        assertThat(savedInvite.getCode(), is(notNullValue()));
        assertThat(notifyPromise.isDone(), is(true));
    }

    @Test
    public void create_shouldStillCreateTheInviteFailingOnSendingEmail() throws Exception {

        mocksCreateInvite();

        CompletableFuture<String> errorPromise = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("some error from notify");
        });

        when(mockNotificationService.sendInviteEmail(eq(senderEmail), eq(email), matches("^http://selfservice/invites/[0-9a-z]{32}$")))
                .thenReturn(errorPromise);

        inviteService.create(inviteRequestFrom(senderExternalId, email, roleName), serviceId);

        verify(mockInviteDao).persist(expectedInvite.capture());
        InviteEntity savedInvite = expectedInvite.getValue();

        assertThat(savedInvite.getEmail(), is(email));
        assertThat(savedInvite.getOtpKey(), is(notNullValue()));
        assertThat(savedInvite.getCode(), is(notNullValue()));
        assertThat(errorPromise.isCompletedExceptionally(), is(true));
    }

    @Test
    public void create_shouldFailWithConflictWhenUserExists() throws Exception {

        String existingUserEmail = "existing-user@example.com";
        User existingUser = aUser(existingUserEmail);
        when(mockUserDao.findByEmail(existingUserEmail)).thenReturn(Optional.of(UserEntity.from(existingUser)));

        InviteUserRequest inviteUserRequest = inviteRequestFrom(senderEmail, existingUserEmail, roleName);
        try {
            inviteService.create(inviteUserRequest, serviceId);
            fail();
        } catch (WebApplicationException e) {
            MatcherAssert.assertThat(e.getResponse().getStatus(), is(CONFLICT.getStatusCode()));
        }
    }

    @Test
    public void create_shouldFailWithConflictWhenInviteExistsAndNotExpiredAndNotDisabled() throws Exception {

        InviteEntity anInvite = mocksCreateInvite();
        when(mockInviteDao.findByEmail(email)).thenReturn(Optional.of(anInvite));

        InviteUserRequest inviteUserRequest = inviteRequestFrom(senderEmail, email, roleName);
        try {
            inviteService.create(inviteUserRequest, serviceId);
            fail();
        } catch (WebApplicationException e) {
            MatcherAssert.assertThat(e.getResponse().getStatus(), is(CONFLICT.getStatusCode()));
        }
    }

    @Test
    public void validateOtpAndCreateUser_shouldCreateInvitedUserOnSuccessfulInvite() throws Exception {

        mocksCreateInvite();
        when(mockSecondFactorAuthenticator.authorize(otpKey, passCode)).thenReturn(true);

        InviteValidateOtpRequest inviteValidateOtpRequest = inviteValidateOtpRequest(inviteCode, passCode);
        inviteService.validateOtpAndCreateUser(inviteValidateOtpRequest);

        verify(mockUserDao).persist(expectedInvitedUser.capture());
        UserEntity createdUser = expectedInvitedUser.getValue();
        assertThat(createdUser.getEmail(), is(email));
        assertThat(createdUser.isDisabled(), is(Boolean.FALSE));
    }

    @Test
    public void validateOtpAndCreateUser_shouldDisableInviteOnSuccessfulInvite() throws Exception {

        mocksCreateInvite();
        when(mockSecondFactorAuthenticator.authorize(otpKey, passCode)).thenReturn(true);

        InviteValidateOtpRequest inviteValidateOtpRequest = inviteValidateOtpRequest(inviteCode, passCode);
        inviteService.validateOtpAndCreateUser(inviteValidateOtpRequest);

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity savedInvite = expectedInvite.getValue();
        assertThat(savedInvite.getCode(), is(inviteCode));
        assertThat(savedInvite.isDisabled(), is(Boolean.TRUE));
    }

    @Test
    public void validateOtpAndCreateUser_shouldErrorWhenDisabled_evenIfOtpValidationIsSuccessful() throws Exception {

        InviteEntity anInvite = mocksCreateInvite();
        anInvite.setDisabled(Boolean.TRUE);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        when(mockSecondFactorAuthenticator.authorize(otpKey, passCode)).thenReturn(true);

        InviteValidateOtpRequest inviteValidateOtpRequest = inviteValidateOtpRequest(inviteCode, passCode);
        ValidateOtpAndCreateUserResult validateOtpAndCreateUserResult =
                inviteService.validateOtpAndCreateUser(inviteValidateOtpRequest);
        assertThat(validateOtpAndCreateUserResult.isError(), is(true));
        assertThat(validateOtpAndCreateUserResult.getError().getResponse().getStatus(), is(GONE.getStatusCode()));
    }

    @Test
    public void validateOtpAndCreateUser_shouldErrorAndDisableWhenInvalidOtpValidation_ifMaxRetryExceeded() throws Exception {

        InviteEntity anInvite = mocksCreateInvite();
        anInvite.setLoginCounter(2);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        int invalidPassCode = 1337;
        when(mockSecondFactorAuthenticator.authorize(otpKey, invalidPassCode)).thenReturn(false);

        InviteValidateOtpRequest inviteValidateOtpRequest = inviteValidateOtpRequest(inviteCode, invalidPassCode);
        ValidateOtpAndCreateUserResult validateOtpAndCreateUserResult =
                inviteService.validateOtpAndCreateUser(inviteValidateOtpRequest);
        assertThat(validateOtpAndCreateUserResult.isError(), is(true));
        assertThat(validateOtpAndCreateUserResult.getError().getResponse().getStatus(), is(GONE.getStatusCode()));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity savedInvite = expectedInvite.getValue();
        assertThat(savedInvite.getLoginCounter(), is(3));
        assertThat(savedInvite.isDisabled(), is(Boolean.TRUE));
    }

    @Test
    public void validateOtpAndCreateUser_shouldErrorAndIncrementLoginCounterWhenInvalidOtpValidation() throws Exception {

        InviteEntity anInvite = mocksCreateInvite();
        anInvite.setLoginCounter(1);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        int invalidPassCode = 1337;
        when(mockSecondFactorAuthenticator.authorize(otpKey, invalidPassCode)).thenReturn(false);

        InviteValidateOtpRequest inviteValidateOtpRequest = inviteValidateOtpRequest(inviteCode, invalidPassCode);
        ValidateOtpAndCreateUserResult validateOtpAndCreateUserResult =
                inviteService.validateOtpAndCreateUser(inviteValidateOtpRequest);
        assertThat(validateOtpAndCreateUserResult.isError(), is(true));
        assertThat(validateOtpAndCreateUserResult.getError().getResponse().getStatus(), is(UNAUTHORIZED.getStatusCode()));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity savedInvite = expectedInvite.getValue();
        assertThat(savedInvite.getLoginCounter(), is(2));
        assertThat(savedInvite.isDisabled(), is(Boolean.FALSE));
    }

    @Test
    public void validateOtpAndCreateUser_shouldCreateInvitedUserAndResetLoginCounterAndDisableInviteWhenValidOtpValidation() throws Exception {

        InviteEntity anInvite = mocksCreateInvite();
        anInvite.setLoginCounter(2);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        when(mockSecondFactorAuthenticator.authorize(otpKey, passCode)).thenReturn(true);

        InviteValidateOtpRequest inviteValidateOtpRequest = inviteValidateOtpRequest(inviteCode, passCode);
        inviteService.validateOtpAndCreateUser(inviteValidateOtpRequest);

        verify(mockUserDao).persist(expectedInvitedUser.capture());
        UserEntity createdUser = expectedInvitedUser.getValue();
        assertThat(createdUser.getEmail(), is(email));
        assertThat(createdUser.isDisabled(), is(Boolean.FALSE));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity savedInvite = expectedInvite.getValue();
        assertThat(savedInvite.getLoginCounter(), is(0));
        assertThat(savedInvite.isDisabled(), is(Boolean.TRUE));
    }

    @Test
    public void validateOtpAndCreateUser_shouldErrorWhenInviteNotFound() throws Exception {

        String notFoundInviteCode = "not-found-invite-code";
        when(mockInviteDao.findByCode(notFoundInviteCode)).thenReturn(Optional.empty());
        InviteValidateOtpRequest inviteValidateOtpRequest = inviteValidateOtpRequest(notFoundInviteCode, passCode);
        ValidateOtpAndCreateUserResult validateOtpAndCreateUserResult =
                inviteService.validateOtpAndCreateUser(inviteValidateOtpRequest);
        assertThat(validateOtpAndCreateUserResult.isError(), is(true));
        assertThat(validateOtpAndCreateUserResult.getError().getResponse().getStatus(), is(NOT_FOUND.getStatusCode()));
    }

    @Test
    public void generateOtp_shouldSendNotificationOnSuccessfulInviteUpdate() {

        String telephoneNumber = "+4498765423";
        String plainPassword = "my-secure-pass";
        String encryptedPassword = "my-secure-pass-encrypted";

        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(mockPasswordHasher.hash(plainPassword)).thenReturn(encryptedPassword);
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        CompletableFuture<String> errorPromise = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("some error from notify");
        });
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(telephoneNumber), eq(valueOf(passCode))))
                .thenReturn(errorPromise);

        inviteService.generateOtp(inviteOtpRequestFrom(inviteCode, telephoneNumber, plainPassword));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(telephoneNumber));
        assertThat(updatedInvite.getPassword(), is(encryptedPassword));
        assertThat(errorPromise.isCompletedExceptionally(), is(true));
    }

    @Test
    public void generateOtp_shouldStillUpdateTheInviteWhen2FAFails() {

        String telephoneNumber = "+4498765423";
        String plainPassword = "my-secure-pass";
        String encryptedPassword = "my-secure-pass-encrypted";

        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(mockPasswordHasher.hash(plainPassword)).thenReturn(encryptedPassword);
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        CompletableFuture<String> notifyPromise = CompletableFuture.completedFuture("random-notify-id");
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(telephoneNumber), eq(valueOf(passCode))))
                .thenReturn(notifyPromise);

        inviteService.generateOtp(inviteOtpRequestFrom(inviteCode, telephoneNumber, plainPassword));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(telephoneNumber));
        assertThat(updatedInvite.getPassword(), is(encryptedPassword));
        assertThat(notifyPromise.isDone(), is(true));
    }

    private InviteUserRequest inviteRequestFrom(String sender, String email, String roleName) {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put(FIELD_SENDER, sender);
        json.put(FIELD_EMAIL, email);
        json.put(FIELD_ROLE_NAME, roleName);
        return InviteUserRequest.from(json);
    }

    private InviteOtpRequest inviteOtpRequestFrom(String code, String telephoneNumber, String password) {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put(FIELD_CODE, code);
        json.put(FIELD_TELEPHONE_NUMBER, telephoneNumber);
        json.put(FIELD_PASSWORD, password);
        return InviteOtpRequest.from(json);
    }

    private InviteValidateOtpRequest inviteValidateOtpRequest(String inviteCode, int otpCode) {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put(InviteValidateOtpRequest.FIELD_CODE, inviteCode);
        json.put(InviteValidateOtpRequest.FIELD_OTP, otpCode);
        return InviteValidateOtpRequest.from(json);
    }

    private InviteEntity anInvite(String email, String code, String otpKey, UserEntity userEntity, ServiceEntity serviceEntity, RoleEntity roleEntity) {
        return new InviteEntity(email, code, otpKey, userEntity, serviceEntity, roleEntity);
    }

    private User aUser(String email) {
        return User.from(randomInt(), randomUuid(), "a-username", "random-password", email, asList("1"), asList(Service.from(serviceId, serviceExternalId, Service.DEFAULT_NAME_VALUE)), "784rh", "8948924");
    }
}
