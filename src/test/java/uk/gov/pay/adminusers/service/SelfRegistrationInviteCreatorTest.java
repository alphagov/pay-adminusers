package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.app.config.LinksConfig;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.CreateSelfRegistrationInviteRequest;
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

@ExtendWith(MockitoExtension.class)
class SelfRegistrationInviteCreatorTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private LinksConfig linksConfig;
    @Mock
    private InviteDao inviteDao;
    @Mock
    private UserDao userDao;
    @Mock
    private RoleDao roleDao;
    @Mock
    private SecondFactorAuthenticator secondFactorAuthenticator;
    @Captor
    private ArgumentCaptor<InviteEntity> persistedInviteEntity;
    private SelfRegistrationInviteCreator selfRegistrationInviteCreator;

    @BeforeEach
    void before() {
        selfRegistrationInviteCreator = new SelfRegistrationInviteCreator(inviteDao, userDao, roleDao,
                new LinksBuilder("http://localhost/"), linksConfig, notificationService,
                secondFactorAuthenticator);
    }

    @Test
    void shouldSuccess_IfEmailDoesNotConflict() {
        String email = "email@example.gov.uk";
        CreateSelfRegistrationInviteRequest request = new CreateSelfRegistrationInviteRequest(email);
        RoleEntity roleEntity = new RoleEntity(Role.role(2, "admin", "Adminstrator"));
        when(userDao.findByEmail(email)).thenReturn(Optional.empty());
        when(inviteDao.findByEmail(email)).thenReturn(emptyList());
        when(roleDao.findByRoleName("admin")).thenReturn(Optional.of(roleEntity));
        when(notificationService.sendSelfRegistrationInviteEmail(eq(email), anyString())).thenReturn("done");
        when(linksConfig.getSelfserviceInvitesUrl()).thenReturn("http://selfservice/invites");

        Invite invite = selfRegistrationInviteCreator.doInvite(request);

        verify(inviteDao, times(1)).persist(persistedInviteEntity.capture());
        assertThat(invite.getEmail(), is(request.getEmail()));
        assertThat(invite.isInviteToJoinService(), is(false));
        assertThat(invite.getType(), is("service"));
        assertThat(invite.getLinks().get(0).getHref(), matchesPattern("^http://selfservice/invites/[0-9a-z]{32}$"));
    }

    @Test
    void shouldSuccess_evenIfNotifyThrowsAnError() {
        String email = "email@example.gov.uk";
        CreateSelfRegistrationInviteRequest request = new CreateSelfRegistrationInviteRequest(email);
        RoleEntity roleEntity = new RoleEntity(Role.role(2, "admin", "Adminstrator"));
        when(userDao.findByEmail(email)).thenReturn(Optional.empty());
        when(inviteDao.findByEmail(email)).thenReturn(emptyList());
        when(roleDao.findByRoleName("admin")).thenReturn(Optional.of(roleEntity));
        when(notificationService.sendSelfRegistrationInviteEmail(eq(email), anyString())).thenThrow(AdminUsersExceptions.userNotificationError(new Exception("Cause")));
        when(linksConfig.getSelfserviceInvitesUrl()).thenReturn("http://selfservice/invites");
        Invite invite = selfRegistrationInviteCreator.doInvite(request);

        verify(inviteDao, times(1)).persist(persistedInviteEntity.capture());
        assertThat(invite.getEmail(), is(request.getEmail()));
        assertThat(invite.isInviteToJoinService(), is(false));
        assertThat(invite.getType(), is("service"));
        assertThat(invite.getLinks().get(0).getHref(), matchesPattern("^http://selfservice/invites/[0-9a-z]{32}$"));

    }

    @Test
    void shouldSuccess_ifUserAlreadyHasAValidSelfRegistrationInvitationWithGivenEmail() {
        String email = "email@example.gov.uk";
        CreateSelfRegistrationInviteRequest request = new CreateSelfRegistrationInviteRequest(email);
        UserEntity sender = mock(UserEntity.class);
        RoleEntity role = mock(RoleEntity.class);
        InviteEntity validInvite = new InviteEntity(email, "code", "otpKey", role);
        validInvite.setSender(sender);
        validInvite.setType(InviteType.SERVICE);

        when(userDao.findByEmail(email)).thenReturn(Optional.empty());
        when(inviteDao.findByEmail(email)).thenReturn(List.of(validInvite));
        when(linksConfig.getSelfserviceInvitesUrl()).thenReturn("http://selfservice/invites");
        when(notificationService.sendSelfRegistrationInviteEmail(eq(email), anyString()))
                .thenReturn("done");

        Invite invite = selfRegistrationInviteCreator.doInvite(request);

        verify(inviteDao, times(1)).merge(persistedInviteEntity.capture());
        assertThat(invite.getEmail(), is(request.getEmail()));
        assertThat(invite.isInviteToJoinService(), is(false));
        assertThat(invite.getType(), is("service"));
        assertThat(invite.getLinks().get(0).getHref(), is("http://selfservice/invites/code"));
    }

    @Test
    void shouldSuccess_evenIfUserAlreadyHasAValidJoinServiceInvitationWithGivenEmail() {
        String email = "email@example.gov.uk";
        CreateSelfRegistrationInviteRequest request = new CreateSelfRegistrationInviteRequest(email);
        UserEntity sender = mock(UserEntity.class);
        ServiceEntity service = mock(ServiceEntity.class);
        RoleEntity role = mock(RoleEntity.class);
        InviteEntity validInvite = new InviteEntity(email, "code", "otpKey", role);
        validInvite.setSender(sender);
        validInvite.setService(service);

        RoleEntity roleEntity = new RoleEntity(Role.role(2, "admin", "Adminstrator"));
        when(roleDao.findByRoleName("admin")).thenReturn(Optional.of(roleEntity));
        when(userDao.findByEmail(email)).thenReturn(Optional.empty());
        when(inviteDao.findByEmail(email)).thenReturn(List.of(validInvite));
        when(linksConfig.getSelfserviceInvitesUrl()).thenReturn("http://selfservice/invites");
        when(notificationService.sendSelfRegistrationInviteEmail(eq(email), matches("^http://selfservice/invites/[0-9a-z]{32}$")))
                .thenReturn("done");

        Invite invite = selfRegistrationInviteCreator.doInvite(request);

        assertThat(invite.getEmail(), is(request.getEmail()));
        assertThat(invite.isInviteToJoinService(), is(false));
        assertThat(invite.getType(), is("service"));
        assertThat(invite.getLinks().get(0).getHref().matches("^http://selfservice/invites/[0-9a-z]{32}$"), is(true));
    }

    @Test
    void shouldError_ifUserAlreadyExistsWithGivenEmail() {
        String email = "email@example.gov.uk";
        CreateSelfRegistrationInviteRequest request = new CreateSelfRegistrationInviteRequest(email);
        UserEntity existingUserEntity = new UserEntity();
        when(userDao.findByEmail(email)).thenReturn(Optional.of(existingUserEntity));
        when(linksConfig.getSupportUrl()).thenReturn("http://frontend");
        when(linksConfig.getSelfserviceForgottenPasswordUrl()).thenReturn("http://selfservice/forgotten-password");
        when(linksConfig.getSelfserviceLoginUrl()).thenReturn("http://selfservice/login");
        when(notificationService.sendSelfRegistrationInviteUserExistsEmail(eq(email), anyString(), anyString(), anyString()))
                .thenReturn("done");

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> selfRegistrationInviteCreator.doInvite(request));
        assertThat(exception.getMessage(), is("HTTP 409 Conflict"));
    }

    @Test
    void shouldError_ifUserAlreadyExistsAndDisabledWithGivenEmail() {
        String email = "email@example.gov.uk";
        CreateSelfRegistrationInviteRequest request = new CreateSelfRegistrationInviteRequest(email);
        UserEntity existingUserEntity = new UserEntity();
        existingUserEntity.setDisabled(true);
        when(userDao.findByEmail(email)).thenReturn(Optional.of(existingUserEntity));
        when(linksConfig.getSupportUrl()).thenReturn("http://frontend");
        when(notificationService.sendSelfRegistrationInviteUserExistsAndIsDisabledEmail(eq(email), anyString()))
                .thenReturn("done");

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> selfRegistrationInviteCreator.doInvite(request));
        assertThat(exception.getMessage(), is("HTTP 409 Conflict"));
    }

    @Test
    void shouldError_ifRoleDoesNotExist() {
        String email = "email@example.gov.uk";
        CreateSelfRegistrationInviteRequest request = new CreateSelfRegistrationInviteRequest(email);
        when(userDao.findByEmail(email)).thenReturn(Optional.empty());
        when(inviteDao.findByEmail(email)).thenReturn(emptyList());
        when(roleDao.findByRoleName("admin")).thenReturn(Optional.empty());

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> selfRegistrationInviteCreator.doInvite(request));
        assertThat(exception.getMessage(), is("HTTP 500 Internal Server Error"));
    }
}
