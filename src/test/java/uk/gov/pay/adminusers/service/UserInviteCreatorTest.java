package uk.gov.pay.adminusers.service;


import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.app.config.LinksConfig;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.InviteUserRequest;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.*;

import javax.ws.rs.WebApplicationException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.*;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.InviteRequest.FIELD_EMAIL;
import static uk.gov.pay.adminusers.model.InviteRequest.FIELD_ROLE_NAME;
import static uk.gov.pay.adminusers.model.InviteUserRequest.FIELD_SENDER;
import static uk.gov.pay.adminusers.model.InviteUserRequest.FIELD_SERVICE_EXTERNAL_ID;
import static uk.gov.pay.adminusers.model.Role.role;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

public class UserInviteCreatorTest {

    private static final String SELFSERVICE_URL = "http://selfservice";

    private RoleDao mockRoleDao = mock(RoleDao.class);
    private ServiceDao mockServiceDao = mock(ServiceDao.class);
    private UserDao mockUserDao = mock(UserDao.class);
    private InviteDao mockInviteDao = mock(InviteDao.class);
    private AdminUsersConfig mockConfig = mock(AdminUsersConfig.class);
    private NotificationService mockNotificationService = mock(NotificationService.class);
    private LinksConfig linksConfig = mock(LinksConfig.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private UserInviteCreator userInviteCreator;
    private ArgumentCaptor<InviteEntity> expectedInvite = ArgumentCaptor.forClass(InviteEntity.class);
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
        userInviteCreator = new UserInviteCreator(mockInviteDao, mockUserDao, mockRoleDao, linksConfig, mockNotificationService, mockServiceDao);
    }

    @Test
    public void create_shouldSendNotificationOnSuccessfulInvite() throws Exception {

        mocksCreateInvite();

        CompletableFuture<String> notifyPromise = CompletableFuture.completedFuture("random-notify-id");

        when(mockNotificationService.sendInviteEmail(eq(senderEmail), eq(email), matches("^http://selfservice/invites/[0-9a-z]{32}$")))
                .thenReturn(notifyPromise);

        userInviteCreator.doInvite(inviteRequestFrom(senderExternalId, email, roleName));

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

        userInviteCreator.doInvite(inviteRequestFrom(senderExternalId, email, roleName));

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

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 409 Conflict");

        userInviteCreator.doInvite(inviteUserRequest);

    }

    @Test
    public void create_shouldFailWithConflictWhenInviteExistsAndNotExpiredAndNotDisabled() throws Exception {

        InviteEntity anInvite = mocksCreateInvite();
        when(mockInviteDao.findByEmail(email)).thenReturn(Optional.of(anInvite));

        InviteUserRequest inviteUserRequest = inviteRequestFrom(senderEmail, email, roleName);
        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 409 Conflict");

        userInviteCreator.doInvite(inviteUserRequest);
    }

    @Test
    public void shouldReturnEmpty_ifServiceNotFound() throws Exception {
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());
        when(mockServiceDao.findByExternalId(serviceExternalId)).thenReturn(Optional.empty());
        when(mockInviteDao.findByEmail(email)).thenReturn(Optional.empty());
        InviteUserRequest inviteUserRequest = inviteRequestFrom(senderEmail, email, roleName);
        Optional<Invite> invite = userInviteCreator.doInvite(inviteUserRequest);

        assertFalse(invite.isPresent());

    }

    private InviteEntity mocksCreateInvite() {

        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);
        service.setExternalId(serviceExternalId);

        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());
        when(mockInviteDao.findByEmail(email)).thenReturn(Optional.empty());
        when(mockServiceDao.findByExternalId(serviceExternalId)).thenReturn(Optional.of(service));
        when(mockRoleDao.findByRoleName(roleName)).thenReturn(Optional.of(new RoleEntity()));
        when(linksConfig.getSelfserviceInvitesUrl()).thenReturn("http://selfservice/invites");

        UserEntity senderUser = new UserEntity();
        senderUser.setExternalId(senderExternalId);
        senderUser.setEmail(senderEmail);
        RoleEntity role = new RoleEntity(role(ADMIN.getId(), "admin", "Admin Role"));
        senderUser.setServiceRole(new ServiceRoleEntity(service, role));
        when(mockUserDao.findByExternalId(senderExternalId)).thenReturn(Optional.of(senderUser));

        String inviteCode = "code";
        InviteEntity anInvite = anInvite(email, inviteCode, "otpKey", senderUser, service, role);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));

        doNothing().when(mockInviteDao).persist(any(InviteEntity.class));

        return anInvite;
    }

    private InviteEntity anInvite(String email, String code, String otpKey, UserEntity userEntity, ServiceEntity serviceEntity, RoleEntity roleEntity) {
        return new InviteEntity(email, code, otpKey, userEntity, serviceEntity, roleEntity);
    }

    private InviteUserRequest inviteRequestFrom(String sender, String email, String roleName) {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put(FIELD_SENDER, sender);
        json.put(FIELD_EMAIL, email);
        json.put(FIELD_ROLE_NAME, roleName);
        json.put(FIELD_SERVICE_EXTERNAL_ID, serviceExternalId);
        return InviteUserRequest.from(json);
    }

    private User aUser(String email) {
        return User.from(randomInt(), randomUuid(), "a-username", "random-password", email, asList("1"), asList(Service.from(serviceId, serviceExternalId, Service.DEFAULT_NAME_VALUE)), "784rh", "8948924");
    }

}
