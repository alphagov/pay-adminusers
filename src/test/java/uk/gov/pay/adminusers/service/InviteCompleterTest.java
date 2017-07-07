package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.model.*;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.*;

import javax.ws.rs.WebApplicationException;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.Role.role;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

@RunWith(MockitoJUnitRunner.class)
public class InviteCompleterTest {
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

    private InviteCompleter inviteCompleter;
    private ArgumentCaptor<UserEntity> expectedInvitedUser = ArgumentCaptor.forClass(UserEntity.class);
    private ArgumentCaptor<InviteEntity> expectedInvite = ArgumentCaptor.forClass(InviteEntity.class);
    private ArgumentCaptor<ServiceEntity> expectedService = ArgumentCaptor.forClass(ServiceEntity.class);

    private String otpKey = "otpKey";
    private String inviteCode = "code";
    private String senderEmail = "sender@example.com";
    private String email = "invited@example.com";
    private int serviceId = 1;
    private String serviceExternalId = "3453rmeuty87t";
    private String senderExternalId = "12345";
    private String roleName = "view-only";
    private String baseUrl = "http://localhost";

    @Before
    public void setup() {
        inviteCompleter = new InviteCompleter(
                mockInviteDao,
                mockUserDao,
                mockServiceDao,
                new LinksBuilder(baseUrl)
        );
    }

    @Test
    public void shouldCreateServiceAndUser_whenPassedValidServiceInviteCode() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);


        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.SERVICE);
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        when(mockRoleDao.findByRoleName(roleName)).thenReturn(Optional.of(new RoleEntity()));

        Invite invite = inviteCompleter.complete(anInvite.getCode()).get();

        verify(mockServiceDao).persist(expectedService.capture());
        verify(mockUserDao).persist(expectedInvitedUser.capture());
        verify(mockInviteDao).persist(expectedInvite.capture());

        assertThat(invite.isDisabled(), is(true));
        assertThat(invite.getLinks().size(), is(1));
        assertThat(invite.getLinks().get(0).getRel().toString(), is("user"));
        assertThat(invite.getLinks().get(0).getHref(), matchesPattern("^" + baseUrl +  "/v1/api/users/[0-9a-z]{32}$"));
    }

    @Test
    public void shouldCreateOnlyUser_whenPassedValidUserInviteCode() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.USER);

        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        when(mockRoleDao.findByRoleName(roleName)).thenReturn(Optional.of(new RoleEntity()));

        Invite invite = inviteCompleter.complete(anInvite.getCode()).get();

        verifyNoMoreInteractions(mockServiceDao);
        verify(mockUserDao).persist(expectedInvitedUser.capture());
        verify(mockInviteDao).persist(expectedInvite.capture());

        assertThat(invite.isDisabled(), is(true));
        assertThat(invite.getLinks().size(), is(1));
        assertThat(invite.getLinks().get(0).getRel().toString(), is("user"));
        assertThat(invite.getLinks().get(0).getHref(), matchesPattern("^" + baseUrl +  "/v1/api/users/[0-9a-z]{32}$"));

    }

    @Test(expected= WebApplicationException.class)
    public void shouldThrowEmailExistsException_whenPassedInviteCodeWithExistingUserEmail() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.USER);

        when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(UserEntity.from(aUser("bob@example.com"))));
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        when(mockRoleDao.findByRoleName(roleName)).thenReturn(Optional.of(new RoleEntity()));

        inviteCompleter.complete(anInvite.getCode());
    }

    @Test(expected= WebApplicationException.class)
    public void shouldThrowEmailExistsException_whenPassedInviteCodeWhichIsDisabled() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.USER);
        anInvite.setDisabled(true);

        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));

        inviteCompleter.complete(anInvite.getCode());
    }

    @Test(expected= WebApplicationException.class)
    public void shouldThrowEmailExistsException_whenPassedInviteCodeWhichIsExpired() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.USER);
        anInvite.setExpiryDate(ZonedDateTime.now().minusDays(1));

        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));

        inviteCompleter.complete(anInvite.getCode());
    }

    private InviteEntity createInvite() {

        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        UserEntity senderUser = new UserEntity();
        senderUser.setExternalId(senderExternalId);
        senderUser.setEmail(senderEmail);
        RoleEntity role = new RoleEntity(role(ADMIN.getId(), "admin", "Admin Role"));
        senderUser.setServiceRole(new ServiceRoleEntity(service, role));

        InviteEntity anInvite = anInvite(email, inviteCode, otpKey, senderUser, service, role);


        return anInvite;
    }

    private InviteEntity anInvite(String email, String code, String otpKey, UserEntity userEntity, ServiceEntity serviceEntity, RoleEntity roleEntity) {
        return new InviteEntity(email, code, otpKey, userEntity, serviceEntity, roleEntity);
    }

    private User aUser(String email) {
        Service service = Service.from(serviceId, serviceExternalId, Service.DEFAULT_NAME_VALUE);
        return User.from(randomInt(), randomUuid(), "a-username", "random-password", email, asList("1"), asList(service), "784rh", "8948924",
                asList(ServiceRole.from(service, role(ADMIN.getId(), "Admin", "Administrator"))));
    }
}
