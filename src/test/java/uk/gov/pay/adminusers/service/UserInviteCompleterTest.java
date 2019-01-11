package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.model.InviteCompleteResponse;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceRole;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.ws.rs.WebApplicationException;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.Role.role;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

@RunWith(MockitoJUnitRunner.class)
public class UserInviteCompleterTest {

    @Mock
    private UserDao mockUserDao;
    @Mock
    private InviteDao mockInviteDao;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private InviteCompleter userInviteCompleter;

    private String otpKey = "otpKey";
    private String inviteCode = "code";
    private String senderEmail = "sender@example.com";
    private String email = "invited@example.com";
    private int serviceId = 1;
    private String serviceExternalId = "3453rmeuty87t";
    private String senderExternalId = "12345";

    @Before
    public void setup() {
        userInviteCompleter = new UserInviteCompleter(
                mockInviteDao,
                mockUserDao
        );
    }

    @Test
    public void shouldSuccess_whenSubscribingAServiceToAnExistingUser_forValidInvite() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);
        service.setExternalId(serviceExternalId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.USER);
        anInvite.setService(service);
        UserEntity user = UserEntity.from(aUser(anInvite.getEmail()));

        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(user));

        Optional<InviteCompleteResponse> completedInvite = userInviteCompleter.complete(inviteCode);

        ArgumentCaptor<UserEntity> persistedUser = ArgumentCaptor.forClass(UserEntity.class);
        verify(mockUserDao).merge(persistedUser.capture());

        assertThat(completedInvite.isPresent(), is(true));
        assertThat(completedInvite.get().getInvite().isDisabled(), is(true));
        assertThat(persistedUser.getValue().getServicesRole(service.getExternalId()).isPresent(), is(true));
    }


    @Test
    public void shouldError_whenSubscribingAServiceToAnExistingUser_ifServiceIsNull() {

        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);
        service.setExternalId(serviceExternalId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.USER);
        anInvite.setService(null);
        UserEntity user = UserEntity.from(aUser(anInvite.getEmail()));

        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(user));

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 500 Internal Server Error");
        userInviteCompleter.complete(inviteCode);
    }

    @Test
    public void shouldError_whenSubscribingAServiceToAnExistingUser_ifInviteIsNotUserType() {

        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);
        service.setExternalId(serviceExternalId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.SERVICE);
        anInvite.setService(service);
        UserEntity user = UserEntity.from(aUser(anInvite.getEmail()));

        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(user));

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 500 Internal Server Error");
        userInviteCompleter.complete(inviteCode);

    }


    @Test
    public void shouldThrowEmailExistsException_whenPassedInviteCodeWhichIsDisabled() {
        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.USER);
        anInvite.setDisabled(true);

        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 410 Gone");
        userInviteCompleter.complete(anInvite.getCode());
    }

    @Test
    public void shouldThrowEmailExistsException_whenPassedInviteCodeWhichIsExpired() {
        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.USER);
        anInvite.setExpiryDate(ZonedDateTime.now().minusDays(1));

        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 410 Gone");
        userInviteCompleter.complete(anInvite.getCode());
    }

    @Test
    public void shouldThrowInternalError_whenUserWithSpecifiedEmailNotExists() {
        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.USER);

        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 500 Internal Server Error");
        userInviteCompleter.complete(anInvite.getCode());
    }

    private InviteEntity createInvite() {

        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        UserEntity senderUser = new UserEntity();
        senderUser.setExternalId(senderExternalId);
        senderUser.setEmail(senderEmail);
        RoleEntity role = new RoleEntity(role(ADMIN.getId(), "admin", "Admin Role"));
        senderUser.addServiceRole(new ServiceRoleEntity(service, role));

        InviteEntity anInvite = anInvite(email, inviteCode, otpKey, senderUser, service, role);
        return anInvite;
    }

    private InviteEntity anInvite(String email, String code, String otpKey, UserEntity userEntity, ServiceEntity serviceEntity, RoleEntity roleEntity) {
        InviteEntity anInvite = new InviteEntity(email, code, otpKey, roleEntity);
        anInvite.setSender(userEntity);
        anInvite.setService(serviceEntity);

        return anInvite;
    }

    private User aUser(String email) {
        Service service = Service.from(serviceId, serviceExternalId, Service.DEFAULT_NAME_VALUE);
        return User.from(randomInt(), randomUuid(), "a-username", "random-password", email, asList("1"), asList(service), "784rh", "8948924",
                asList(ServiceRole.from(service, role(ADMIN.getId(), "Admin", "Administrator"))), null,
                SecondFactorMethod.SMS, null, null, null);
    }
}
