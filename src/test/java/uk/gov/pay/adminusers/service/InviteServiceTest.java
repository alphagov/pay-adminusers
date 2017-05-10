package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.app.config.LinksConfig;
import uk.gov.pay.adminusers.model.InviteOtpRequest;
import uk.gov.pay.adminusers.model.InviteRequest;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_CODE;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_PASSWORD;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_TELEPHONE_NUMBER;
import static uk.gov.pay.adminusers.model.InviteRequest.*;
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
    private String senderEmail = "sender@example.com";
    private String email = "invited@example.com";
    private int serviceId = 1;
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
                new LinksBuilder("http://localhost")
        );
    }

    private void mocksCreateInvite() {

        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());
        when(mockInviteDao.findByEmail(email)).thenReturn(Optional.empty());
        when(mockServiceDao.findById(serviceId)).thenReturn(Optional.of(service));
        when(mockRoleDao.findByRoleName(roleName)).thenReturn(Optional.of(new RoleEntity()));

        UserEntity senderUser = new UserEntity();
        senderUser.setExternalId(senderExternalId);
        senderUser.setEmail(senderEmail);
        senderUser.setServiceRole(new ServiceRoleEntity(service, new RoleEntity(role(ADMIN.getId(), "admin", "Admin Role"))));
        when(mockUserDao.findByExternalId(senderExternalId)).thenReturn(Optional.of(senderUser));
        doNothing().when(mockInviteDao).persist(any(InviteEntity.class));
    }

    @Test
    public void create_shouldSendNotificationOnSuccessfulInvite() throws Exception {

        mocksCreateInvite();

        CompletableFuture<String> notifyPromise = CompletableFuture.completedFuture("random-notify-id");

        when(mockNotificationService.sendInviteEmail(eq(senderEmail), eq(email), matches("^http://selfservice/invites/[0-9a-z]{20,30}$")))
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

        when(mockNotificationService.sendInviteEmail(eq(senderEmail), eq(email), matches("^http://selfservice/invites/[0-9a-z]{20,30}$")))
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
    public void generateOtp_shouldSendNotificationOnSuccessfulInviteUpdate() {

        String code = "code";
        String telephoneNumber = "+4498765423";
        String plainPassword = "my-secure-pass";
        String encryptedPassword = "my-secure-pass-encrypted";
        String otpKey = "otpKey";
        int passcode = 123456;

        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        when(mockInviteDao.findByCode(code)).thenReturn(Optional.of(inviteEntity));
        when(mockPasswordHasher.hash(plainPassword)).thenReturn(encryptedPassword);
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passcode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        CompletableFuture<String> errorPromise = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("some error from notify");
        });
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(telephoneNumber), eq(valueOf(passcode))))
                .thenReturn(errorPromise);

        inviteService.generateOtp(inviteOtpRequestFrom(code, telephoneNumber, plainPassword));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(telephoneNumber));
        assertThat(updatedInvite.getPassword(), is(encryptedPassword));
        assertThat(errorPromise.isCompletedExceptionally(), is(true));
    }

    @Test
    public void generateOtp_shouldStillUpdateTheInviteWhen2FAFails() {

        String code = "code";
        String telephoneNumber = "+4498765423";
        String plainPassword = "my-secure-pass";
        String encryptedPassword = "my-secure-pass-encrypted";
        String otpKey = "otpKey";
        int passcode = 123456;

        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        when(mockInviteDao.findByCode(code)).thenReturn(Optional.of(inviteEntity));
        when(mockPasswordHasher.hash(plainPassword)).thenReturn(encryptedPassword);
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passcode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        CompletableFuture<String> notifyPromise = CompletableFuture.completedFuture("random-notify-id");
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(telephoneNumber), eq(valueOf(passcode))))
                .thenReturn(notifyPromise);

        inviteService.generateOtp(inviteOtpRequestFrom(code, telephoneNumber, plainPassword));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(telephoneNumber));
        assertThat(updatedInvite.getPassword(), is(encryptedPassword));
        assertThat(notifyPromise.isDone(), is(true));
    }

    private InviteRequest inviteRequestFrom(String sender, String email, String roleName) {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put(FIELD_SENDER, sender);
        json.put(FIELD_EMAIL, email);
        json.put(FIELD_ROLE_NAME, roleName);
        return InviteRequest.from(json);
    }

    private InviteOtpRequest inviteOtpRequestFrom(String code, String telephoneNumber, String password) {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put(FIELD_CODE, code);
        json.put(FIELD_TELEPHONE_NUMBER, telephoneNumber);
        json.put(FIELD_PASSWORD, password);
        return InviteOtpRequest.from(json);
    }
}
