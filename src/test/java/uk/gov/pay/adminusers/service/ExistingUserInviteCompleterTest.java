package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.model.InviteCompleteResponse;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceName;
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
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.Role.role;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

@ExtendWith(MockitoExtension.class)
public class ExistingUserInviteCompleterTest {

    @Mock
    private UserDao mockUserDao;
    @Mock
    private InviteDao mockInviteDao;

    private InviteCompleter existingUserInviteCompleter;

    private String otpKey = "otpKey";
    private String inviteCode = "code";
    private String senderEmail = "sender@example.com";
    private String email = "invited@example.com";
    private int serviceId = 1;
    private String serviceExternalId = "3453rmeuty87t";
    private String senderExternalId = "12345";

    @BeforeEach
    public void setUp() {
        existingUserInviteCompleter = new ExistingUserInviteCompleter(
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

        when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(user));

        InviteCompleteResponse completedInvite = existingUserInviteCompleter.complete(anInvite);

        ArgumentCaptor<UserEntity> persistedUser = ArgumentCaptor.forClass(UserEntity.class);
        verify(mockUserDao).merge(persistedUser.capture());

        assertThat(completedInvite.getInvite().isDisabled(), is(true));
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

        when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(user));

        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
                () -> existingUserInviteCompleter.complete(anInvite));
        assertThat(webApplicationException.getMessage(), is("HTTP 500 Internal Server Error"));
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

        when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(user));

        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
                () -> existingUserInviteCompleter.complete(anInvite));
        assertThat(webApplicationException.getMessage(), is("HTTP 500 Internal Server Error"));
    }


    @Test
    public void shouldThrowEmailExistsException_whenPassedInviteCodeWhichIsDisabled() {
        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.USER);
        anInvite.setDisabled(true);

        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
                () -> existingUserInviteCompleter.complete(anInvite));
        assertThat(webApplicationException.getMessage(), is("HTTP 410 Gone"));
    }

    @Test
    public void shouldThrowEmailExistsException_whenPassedInviteCodeWhichIsExpired() {
        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.USER);
        anInvite.setExpiryDate(ZonedDateTime.now().minusDays(1));

        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
                () -> existingUserInviteCompleter.complete(anInvite));
        assertThat(webApplicationException.getMessage(), is("HTTP 410 Gone"));
    }

    @Test
    public void shouldThrowInternalError_whenUserWithSpecifiedEmailNotExists() {
        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.USER);
        
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());

        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
                () -> existingUserInviteCompleter.complete(anInvite));
        assertThat(webApplicationException.getMessage(), is("HTTP 500 Internal Server Error"));
    }

    @Test
    public void shouldSuccess_whenSubscribingAServiceToAnExistingUser_forValidInvite__newEnumValue() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);
        service.setExternalId(serviceExternalId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.EXISTING_USER_INVITED_TO_EXISTING_SERVICE);
        anInvite.setService(service);
        UserEntity user = UserEntity.from(aUser(anInvite.getEmail()));

        when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(user));

        InviteCompleteResponse completedInvite = existingUserInviteCompleter.complete(anInvite);

        ArgumentCaptor<UserEntity> persistedUser = ArgumentCaptor.forClass(UserEntity.class);
        verify(mockUserDao).merge(persistedUser.capture());

        assertThat(completedInvite.getInvite().isDisabled(), is(true));
        assertThat(persistedUser.getValue().getServicesRole(service.getExternalId()).isPresent(), is(true));
    }

    @Test
    public void shouldError_whenSubscribingAServiceToAnExistingUser_ifServiceIsNull__newEnumValue() {

        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);
        service.setExternalId(serviceExternalId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.EXISTING_USER_INVITED_TO_EXISTING_SERVICE);
        anInvite.setService(null);
        UserEntity user = UserEntity.from(aUser(anInvite.getEmail()));
        
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(user));

        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
                () -> existingUserInviteCompleter.complete(anInvite));
        assertThat(webApplicationException.getMessage(), is("HTTP 500 Internal Server Error"));
    }

    @Test
    public void shouldError_whenSubscribingAServiceToAnExistingUser_ifInviteIsNotExistingUserType() {

        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);
        service.setExternalId(serviceExternalId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.NEW_USER_INVITED_TO_EXISTING_SERVICE);
        anInvite.setService(service);
        UserEntity user = UserEntity.from(aUser(anInvite.getEmail()));

        when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(user));

        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
                () -> existingUserInviteCompleter.complete(anInvite));
        assertThat(webApplicationException.getMessage(), is("HTTP 500 Internal Server Error"));
    }


    @Test
    public void shouldThrowEmailExistsException_whenPassedInviteCodeWhichIsDisabled__newEnumValue() {
        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.EXISTING_USER_INVITED_TO_EXISTING_SERVICE);
        anInvite.setDisabled(true);

        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
                () -> existingUserInviteCompleter.complete(anInvite));
        assertThat(webApplicationException.getMessage(), is("HTTP 410 Gone"));
    }

    @Test
    public void shouldThrowEmailExistsException_whenPassedInviteCodeWhichIsExpired__newEnumValue() {
        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.EXISTING_USER_INVITED_TO_EXISTING_SERVICE);
        anInvite.setExpiryDate(ZonedDateTime.now().minusDays(1));

        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
                () -> existingUserInviteCompleter.complete(anInvite));
        assertThat(webApplicationException.getMessage(), is("HTTP 410 Gone"));
    }

    @Test
    public void shouldThrowInternalError_whenUserWithSpecifiedEmailNotExists__newEnumValue() {
        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.EXISTING_USER_INVITED_TO_EXISTING_SERVICE);
        
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());

        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
                () -> existingUserInviteCompleter.complete(anInvite));
        assertThat(webApplicationException.getMessage(), is("HTTP 500 Internal Server Error"));
    }

    private InviteEntity createInvite() {

        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        UserEntity senderUser = new UserEntity();
        senderUser.setExternalId(senderExternalId);
        senderUser.setEmail(senderEmail);
        RoleEntity role = new RoleEntity(role(ADMIN.getId(), "admin", "Admin Role"));
        senderUser.addServiceRole(new ServiceRoleEntity(service, role));

        return anInvite(email, inviteCode, otpKey, senderUser, service, role);
    }

    private InviteEntity anInvite(String email, String code, String otpKey, UserEntity userEntity, ServiceEntity serviceEntity, RoleEntity roleEntity) {
        InviteEntity anInvite = new InviteEntity(email, code, otpKey, roleEntity);
        anInvite.setSender(userEntity);
        anInvite.setService(serviceEntity);

        return anInvite;
    }

    private User aUser(String email) {
        Service service = Service.from(serviceId, serviceExternalId, new ServiceName(Service.DEFAULT_NAME_VALUE));
        ServiceRole serviceRole = ServiceRole.from(service, role(ADMIN.getId(), "Admin", "Administrator"));
        return User.from(randomInt(), randomUuid(), "a-username", "random-password", email,
                "784rh", "8948924", Collections.singletonList(serviceRole), null,
                SecondFactorMethod.SMS, null, null, null);
    }
}
