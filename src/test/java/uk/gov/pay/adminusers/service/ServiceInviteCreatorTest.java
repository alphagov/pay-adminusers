package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.pay.adminusers.app.config.LinksConfig;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.InviteServiceRequest;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class ServiceInviteCreatorTest {
    private NotificationService notificationService = mock(NotificationService.class);
    private LinksConfig linksConfig = mock(LinksConfig.class);
    private InviteDao inviteDao = mock(InviteDao.class);
    private UserDao userDao = mock(UserDao.class);
    private RoleDao roleDao = mock(RoleDao.class);
    private PasswordHasher passwordHasher = mock(PasswordHasher.class);
    private ArgumentCaptor<InviteEntity> persistedInviteEntity = ArgumentCaptor.forClass(InviteEntity.class);
    private ServiceInviteCreator serviceInviteCreator;

    @BeforeEach
    public void before() {
        serviceInviteCreator = new ServiceInviteCreator(inviteDao, userDao, roleDao, new LinksBuilder("http://localhost/"), linksConfig, notificationService, passwordHasher);
    }

    @Test
    public void shouldSuccess_serviceInvite_IfEmailDoesNotConflict() {
        String email = "email@example.gov.uk";
        InviteServiceRequest request = new InviteServiceRequest("password", email, "01134960000");
        RoleEntity roleEntity = new RoleEntity(Role.role(2, "admin", "Adminstrator"));
        when(userDao.findByEmail(email)).thenReturn(Optional.empty());
        when(inviteDao.findByEmail(email)).thenReturn(emptyList());
        when(roleDao.findByRoleName("admin")).thenReturn(Optional.of(roleEntity));
        when(notificationService.sendServiceInviteEmail(eq(email), anyString())).thenReturn("done");
        when(linksConfig.getSelfserviceInvitesUrl()).thenReturn("http://selfservice/invites");
        when(linksConfig.getSelfserviceUrl()).thenReturn("http://selfservice");
        when(passwordHasher.hash("password")).thenReturn("encrypted-password");

        Invite invite = serviceInviteCreator.doInvite(request);

        verify(inviteDao, times(1)).persist(persistedInviteEntity.capture());
        assertThat(invite.getEmail(), is(request.getEmail()));
        assertThat(invite.getTelephoneNumber(), is("+441134960000"));
        assertThat(invite.getType(), is("service"));
        assertThat(invite.getLinks().get(0).getHref(), matchesPattern("^http://selfservice/invites/[0-9a-z]{32}$"));

        assertThat(persistedInviteEntity.getValue().getPassword(), is("encrypted-password"));
    }

    @Test
    public void shouldSuccess_serviceInvite_evenIfNotifyThrowsAnError() {
        String email = "email@example.gov.uk";
        InviteServiceRequest request = new InviteServiceRequest("password", email, "01134960000");
        RoleEntity roleEntity = new RoleEntity(Role.role(2, "admin", "Adminstrator"));
        when(userDao.findByEmail(email)).thenReturn(Optional.empty());
        when(inviteDao.findByEmail(email)).thenReturn(emptyList());
        when(roleDao.findByRoleName("admin")).thenReturn(Optional.of(roleEntity));
        when(notificationService.sendServiceInviteEmail(eq(email), anyString())).thenThrow(AdminUsersExceptions.userNotificationError());
        when(linksConfig.getSelfserviceUrl()).thenReturn("http://selfservice");
        when(linksConfig.getSelfserviceInvitesUrl()).thenReturn("http://selfservice/invites");
        Invite invite = serviceInviteCreator.doInvite(request);

        verify(inviteDao, times(1)).persist(persistedInviteEntity.capture());
        assertThat(invite.getEmail(), is(request.getEmail()));
        assertThat(invite.getTelephoneNumber(), is("+441134960000"));
        assertThat(invite.getType(), is("service"));
        assertThat(invite.getLinks().get(0).getHref(), matchesPattern("^http://selfservice/invites/[0-9a-z]{32}$"));

    }

    @Test
    public void shouldSuccess_ifUserAlreadyHasAValidServiceInvitationWithGivenEmail() {
        String email = "email@example.gov.uk";
        InviteServiceRequest request = new InviteServiceRequest("password", email, "01134960000");
        UserEntity sender = mock(UserEntity.class);
        ServiceEntity service = mock(ServiceEntity.class);
        RoleEntity role = mock(RoleEntity.class);
        InviteEntity validInvite = new InviteEntity(email, "code", "otpKey", role);
        validInvite.setService(service);
        validInvite.setSender(sender);
        validInvite.setType(InviteType.SERVICE);

        when(userDao.findByEmail(email)).thenReturn(Optional.empty());
        when(sender.getExternalId()).thenReturn("inviter-id");
        when(sender.getEmail()).thenReturn("inviter@example.com");
        when(inviteDao.findByEmail(email)).thenReturn(List.of(validInvite));
        when(linksConfig.getSelfserviceInvitesUrl()).thenReturn("http://selfservice/invites");
        when(notificationService.sendServiceInviteEmail(eq(email), anyString()))
                .thenReturn("done");

        Invite invite = serviceInviteCreator.doInvite(request);

        verify(inviteDao, times(1)).merge(persistedInviteEntity.capture());
        assertThat(invite.getEmail(), is(request.getEmail()));
        assertThat(invite.getType(), is("service"));
        assertThat(invite.getLinks().get(0).getHref(), is("http://selfservice/invites/code"));
    }

    @Test
    public void shouldSuccess_serviceInvite_evenIfUserAlreadyHasAValidUserInvitationWithGivenEmail() {
        String email = "email@example.gov.uk";
        InviteServiceRequest request = new InviteServiceRequest("password", email, "01134960000");
        UserEntity sender = mock(UserEntity.class);
        ServiceEntity service = mock(ServiceEntity.class);
        RoleEntity role = mock(RoleEntity.class);
        InviteEntity validInvite = new InviteEntity(email, "code", "otpKey", role);
        validInvite.setSender(sender);
        validInvite.setService(service);

        RoleEntity roleEntity = new RoleEntity(Role.role(2, "admin", "Adminstrator"));
        when(roleDao.findByRoleName("admin")).thenReturn(Optional.of(roleEntity));
        when(userDao.findByEmail(email)).thenReturn(Optional.empty());
        when(sender.getExternalId()).thenReturn("inviter-id");
        when(sender.getEmail()).thenReturn("inviter@example.com");
        when(inviteDao.findByEmail(email)).thenReturn(List.of(validInvite));
        when(linksConfig.getSelfserviceInvitesUrl()).thenReturn("http://selfservice/invites");
        when(notificationService.sendServiceInviteEmail(eq(email), matches("^http://selfservice/invites/[0-9a-z]{32}$")))
                .thenReturn("done");

        Invite invite = serviceInviteCreator.doInvite(request);

        assertThat(invite.getEmail(), is(request.getEmail()));
        assertThat(invite.getType(), is("service"));
        assertThat(invite.getLinks().get(0).getHref().matches("^http://selfservice/invites/[0-9a-z]{32}$"), is(true));
    }

    @Test
    public void shouldError_ifUserAlreadyExistsWithGivenEmail() {
        String email = "email@example.gov.uk";
        InviteServiceRequest request = new InviteServiceRequest("password", email, "01134960000");
        UserEntity existingUserEntity = new UserEntity();
        when(userDao.findByEmail(email)).thenReturn(Optional.of(existingUserEntity));
        when(linksConfig.getSupportUrl()).thenReturn("http://frontend");
        when(linksConfig.getSelfserviceForgottenPasswordUrl()).thenReturn("http://selfservice/forgotten-password");
        when(linksConfig.getSelfserviceInvitesUrl()).thenReturn("http://selfservice/invites");
        when(linksConfig.getSelfserviceLoginUrl()).thenReturn("http://selfservice/login");
        when(linksConfig.getSelfserviceUrl()).thenReturn("http://selfservice");
        when(notificationService.sendServiceInviteUserExistsEmail(eq(email), anyString(), anyString(), anyString()))
                .thenReturn("done");

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> serviceInviteCreator.doInvite(request));
        assertThat(exception.getMessage(), is("HTTP 409 Conflict"));
    }

    @Test
    public void shouldError_ifUserAlreadyExistsAndDisabledWithGivenEmail() {
        String email = "email@example.gov.uk";
        InviteServiceRequest request = new InviteServiceRequest("password", email, "01134960000");
        UserEntity existingUserEntity = new UserEntity();
        existingUserEntity.setDisabled(true);
        when(userDao.findByEmail(email)).thenReturn(Optional.of(existingUserEntity));
        when(linksConfig.getSupportUrl()).thenReturn("http://frontend");
        when(notificationService.sendServiceInviteUserDisabledEmail(eq(email), anyString()))
                .thenReturn("done");

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> serviceInviteCreator.doInvite(request));
        assertThat(exception.getMessage(), is("HTTP 409 Conflict"));
    }

    @Test
    public void shouldError_ifRoleDoesNotExists() {
        String email = "email@example.gov.uk";
        InviteServiceRequest request = new InviteServiceRequest("password", email, "01134960000");
        when(userDao.findByEmail(email)).thenReturn(Optional.empty());
        when(inviteDao.findByEmail(email)).thenReturn(emptyList());
        when(roleDao.findByRoleName("admin")).thenReturn(Optional.empty());

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> serviceInviteCreator.doInvite(request));
        assertThat(exception.getMessage(), is("HTTP 500 Internal Server Error"));
    }
}
