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
import uk.gov.pay.adminusers.model.CreateInviteToJoinServiceRequest;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceName;
import uk.gov.pay.adminusers.model.ServiceRole;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import jakarta.ws.rs.WebApplicationException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

@ExtendWith(MockitoExtension.class)
class JoinServiceInviteCreatorTest {
    
    @Mock
    private RoleDao mockRoleDao;
    @Mock
    private ServiceDao mockServiceDao;
    @Mock
    private UserDao mockUserDao;
    @Mock
    private InviteDao mockInviteDao;
    @Mock
    private NotificationService mockNotificationService;
    @Mock
    private SecondFactorAuthenticator secondFactorAuthenticator;
    @Mock
    private LinksConfig linksConfig;

    private JoinServiceInviteCreator joinServiceInviteCreator;
    @Captor
    private ArgumentCaptor<InviteEntity> expectedInvite;
    private String senderEmail = "sender@example.com";
    private String email = "invited@example.com";
    private int serviceId = 1;
    private String serviceExternalId = "3453rmeuty87t";
    private String senderExternalId = "12345";
    private Role viewRole = new Role(4, RoleName.VIEW_ONLY, "View only");

    @BeforeEach
    void setUp() {
        joinServiceInviteCreator = new JoinServiceInviteCreator(mockInviteDao, mockUserDao, mockRoleDao, linksConfig,
                mockNotificationService, mockServiceDao, secondFactorAuthenticator);
    }

    @Test
    void create_shouldSendNotificationOnSuccessfulInvite() {

        mockInviteSuccessForNonExistingUserNonExistingInvite();

        when(mockNotificationService.sendInviteNewUserToJoinServiceEmail(eq(senderEmail), eq(email), matches("^http://selfservice/invites/[0-9a-z]{32}$")))
                .thenReturn("random-notify-id");
        when(linksConfig.getSelfserviceInvitesUrl()).thenReturn("http://selfservice/invites");
        String otpKey = "an-otp-key";
        when(secondFactorAuthenticator.generateNewBase32EncodedSecret()).thenReturn(otpKey);

        joinServiceInviteCreator.doInvite(new CreateInviteToJoinServiceRequest(senderExternalId, email, viewRole.getRoleName(), serviceExternalId));

        verify(mockInviteDao).persist(expectedInvite.capture());
        InviteEntity savedInvite = expectedInvite.getValue();

        assertThat(savedInvite.getEmail(), is(email));
        assertThat(savedInvite.getOtpKey(), is(otpKey));
        assertThat(savedInvite.getCode(), is(notNullValue()));
        assertThat(savedInvite.isInviteToJoinService(), is(true));
    }

    @Test
    void shouldReturnEmpty_ifServiceNotFound() {
        when(mockServiceDao.findByExternalId(serviceExternalId)).thenReturn(Optional.empty());
        var createInviteToJoinServiceRequest = new CreateInviteToJoinServiceRequest(senderExternalId, email, 
                viewRole.getRoleName(), serviceExternalId);
        Optional<Invite> invite = joinServiceInviteCreator.doInvite(createInviteToJoinServiceRequest);

        assertFalse(invite.isPresent());
    }

    @Test
    void create_shouldStillCreateTheInviteFailingOnSendingEmail() {

        mockInviteSuccessForNonExistingUserNonExistingInvite();

        when(mockNotificationService.sendInviteNewUserToJoinServiceEmail(eq(senderEmail), eq(email), matches("^http://selfservice/invites/[0-9a-z]{32}$")))
                .thenThrow(AdminUsersExceptions.userNotificationError(new Exception("Cause")));
        when(linksConfig.getSelfserviceInvitesUrl()).thenReturn("http://selfservice/invites");
        String otpKey = "an-otp-key";
        when(secondFactorAuthenticator.generateNewBase32EncodedSecret()).thenReturn(otpKey);

        joinServiceInviteCreator.doInvite(new CreateInviteToJoinServiceRequest(senderExternalId, email, 
                viewRole.getRoleName(), serviceExternalId));

        verify(mockInviteDao).persist(expectedInvite.capture());
        InviteEntity savedInvite = expectedInvite.getValue();

        assertThat(savedInvite.getEmail(), is(email));
        assertThat(savedInvite.getOtpKey(), is(otpKey));
        assertThat(savedInvite.getCode(), is(notNullValue()));
        assertThat(savedInvite.isInviteToJoinService(), is(true));
    }

    @Test
    void create_shouldFailWithConflict_WhenValidInviteExistsInvitingUserIsDifferent() {

        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);
        service.setExternalId(serviceExternalId);
        String inviteCode = "code";

        UserEntity someOtherSender = new UserEntity();
        String someOtherSenderId = "7834ny0t7cr";
        someOtherSender.setExternalId(someOtherSenderId);
        someOtherSender.setEmail(senderEmail);
        RoleEntity role = new RoleEntity(new Role(ADMIN.getId(), RoleName.ADMIN, "Admin Role"));
        someOtherSender.addServiceRole(new ServiceRoleEntity(service, role));

        when(mockServiceDao.findByExternalId(serviceExternalId)).thenReturn(Optional.of(service));
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());

        InviteEntity anInvite = anInvite(email, inviteCode, "otpKey", someOtherSender, service, role);
        
        when(mockInviteDao.findByEmail(email)).thenReturn(List.of(anInvite));
        var createInviteToJoinServiceRequest = new CreateInviteToJoinServiceRequest(senderExternalId, email, 
                viewRole.getRoleName(), serviceExternalId);

        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class, ()
                -> joinServiceInviteCreator.doInvite(createInviteToJoinServiceRequest));
        assertThat(webApplicationException.getMessage(), is("HTTP 409 Conflict"));
    }

    @Test
    void create_shouldFailWithPreConditionFailed_ifUserAlreadyInService() {

        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);
        service.setExternalId(serviceExternalId);

        UserEntity existingUser = new UserEntity();
        existingUser.setExternalId("7834ny0t7cr");
        existingUser.setEmail(email);
        RoleEntity role = new RoleEntity(new Role(ADMIN.getId(), RoleName.ADMIN, "Admin Role"));
        existingUser.addServiceRole(new ServiceRoleEntity(service, role));

        when(mockServiceDao.findByExternalId(serviceExternalId)).thenReturn(Optional.of(service));
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(existingUser));

        var createInviteToJoinServiceRequest = new CreateInviteToJoinServiceRequest(senderExternalId, email, 
                viewRole.getRoleName(), serviceExternalId);
        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
                () -> joinServiceInviteCreator.doInvite(createInviteToJoinServiceRequest));
        assertThat(webApplicationException.getMessage(), is("HTTP 412 Precondition Failed"));
    }

    @Test
    void create_shouldResendTheSameInviteEmail_ifAValidInviteExistsForTheSameServiceBySameSender_forNewUser() {

        //Given
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());
        InviteEntity anInvite = mockInviteSuccessExistingInvite();
        when(mockNotificationService.sendInviteNewUserToJoinServiceEmail(eq(senderEmail), eq(email), matches("^http://selfservice/invites/[0-9a-z]{32}$")))
                .thenReturn("random-notify-id");
        
        //When
        var createInviteToJoinServiceRequest = new CreateInviteToJoinServiceRequest(senderExternalId, email, 
                viewRole.getRoleName(), serviceExternalId);
        Optional<Invite> invite = joinServiceInviteCreator.doInvite(createInviteToJoinServiceRequest);

        //Then
        assertThat(invite.isPresent(), is(true));
        assertThat(invite.get().getCode(), is(anInvite.getCode()));
        assertThat(invite.get().getEmail(), is(anInvite.getEmail()));
        assertThat(invite.get().isInviteToJoinService(), is(true));
    }

    @Test
    void create_shouldErrorForbidden_ifSenderCannotInviteUsersToTheSpecifiedService() {
        InviteEntity inviteEntity = mockInviteSuccessForNonExistingUserNonExistingInvite();
        inviteEntity.getSender().getServicesRoles().clear();

        var createInviteToJoinServiceRequest = new CreateInviteToJoinServiceRequest(senderExternalId, email, 
                viewRole.getRoleName(), serviceExternalId);
        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
                () -> joinServiceInviteCreator.doInvite(createInviteToJoinServiceRequest));
        assertThat(webApplicationException.getMessage(), is("HTTP 403 Forbidden"));
    }

    @Test
    void create_shouldResendTheSameInviteEmail_ifAValidInviteExistsForTheSameServiceBySameSender_forExistingUser() {

        //Given
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(UserEntity.from(aUser(email))));
        InviteEntity anInvite = mockInviteSuccessExistingInvite();
        when(mockNotificationService.sendInviteExistingUserToJoinServiceEmail(eq(senderEmail), eq(email), matches("^http://selfservice/invites/[0-9a-z]{32}$"),
                eq(anInvite.getService().get().getServiceNames().get(SupportedLanguage.ENGLISH).getName()))).thenReturn("random-notify-id");

        var createInviteToJoinServiceRequest = new CreateInviteToJoinServiceRequest(senderExternalId, email, 
                viewRole.getRoleName(), serviceExternalId);
        Optional<Invite> invite = joinServiceInviteCreator.doInvite(createInviteToJoinServiceRequest);

        assertThat(invite.isPresent(), is(true));
        assertThat(invite.get().getCode(), is(anInvite.getCode()));
        assertThat(invite.get().getEmail(), is(anInvite.getEmail()));
        assertThat(invite.get().isInviteToJoinService(), is(true));
    }

    @Test
    void create_shouldResendTheSameInviteEmail_ifAValidInviteExistsForTheSameServiceBySameSender_forExistingUser_evenIfNotifyThrowsAnError() {

        //Given
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(UserEntity.from(aUser(email))));
        InviteEntity anInvite = mockInviteSuccessExistingInvite();
        when(mockNotificationService.sendInviteExistingUserToJoinServiceEmail(eq(senderEmail), eq(email), matches("^http://selfservice/invites/[0-9a-z]{32}$"),
                eq(anInvite.getService().get().getServiceNames().get(SupportedLanguage.ENGLISH).getName())))
                .thenThrow(AdminUsersExceptions.userNotificationError(new Exception("Cause")));

        var createInviteToJoinServiceRequest = new CreateInviteToJoinServiceRequest(senderExternalId, email, 
                viewRole.getRoleName(), serviceExternalId);
        Optional<Invite> invite = joinServiceInviteCreator.doInvite(createInviteToJoinServiceRequest);

        assertThat(invite.isPresent(), is(true));
        assertThat(invite.get().getCode(), is(anInvite.getCode()));
        assertThat(invite.get().getEmail(), is(anInvite.getEmail()));
        assertThat(invite.get().isInviteToJoinService(), is(true));
    }

    @Test
    void create_shouldOnlyConsider_nonExpiredNonDisabledInviteToSameService_whenCheckingForExistingInvite() {

        InviteEntity validInvite = mockInviteSuccessExistingInvite();
        InviteEntity expiredInvite = new InviteEntity();
        expiredInvite.setExpiryDate(ZonedDateTime.now().minusDays(2));
        expiredInvite.setService(validInvite.getService().get());

        InviteEntity disabledInvite = new InviteEntity();
        disabledInvite.setDisabled(true);
        disabledInvite.setExpiryDate(ZonedDateTime.now().plusDays(1));
        disabledInvite.setService(validInvite.getService().get());

        InviteEntity emptyServiceInvite = new InviteEntity();
        emptyServiceInvite.setExpiryDate(ZonedDateTime.now().plusDays(1));

        InviteEntity nonMatchingServiceInvite = new InviteEntity();
        ServiceEntity serviceEntity = ServiceEntity.from(Service.from(new ServiceName("another-service")));
        nonMatchingServiceInvite.setService(serviceEntity);
        nonMatchingServiceInvite.setExpiryDate(ZonedDateTime.now().plusDays(1));

        when(mockInviteDao.findByEmail(email)).thenReturn(List.of(expiredInvite, disabledInvite, emptyServiceInvite, nonMatchingServiceInvite, validInvite));

        when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(UserEntity.from(aUser(email))));
        when(mockNotificationService.sendInviteExistingUserToJoinServiceEmail(eq(senderEmail), eq(email), matches("^http://selfservice/invites/[0-9a-z]{32}$"),
                eq(validInvite.getService().get().getServiceNames().get(SupportedLanguage.ENGLISH).getName()))).thenReturn("random-notify-id");

        var createInviteToJoinServiceRequest = new CreateInviteToJoinServiceRequest(senderExternalId, email, 
                viewRole.getRoleName(), serviceExternalId);
        Optional<Invite> invite = joinServiceInviteCreator.doInvite(createInviteToJoinServiceRequest);

        assertThat(invite.isPresent(), is(true));
        assertThat(invite.get().getCode(), is(validInvite.getCode()));
        assertThat(invite.get().getEmail(), is(validInvite.getEmail()));
        assertThat(invite.get().isInviteToJoinService(), is(true));
    }

    private InviteEntity mockInviteSuccessExistingInvite() {
        ServiceEntity service = new ServiceEntity();
        service.addOrUpdateServiceName(ServiceNameEntity.from(SupportedLanguage.ENGLISH, Service.DEFAULT_NAME_VALUE));
        service.setId(serviceId);
        service.setExternalId(serviceExternalId);

        UserEntity sameSender = new UserEntity();
        sameSender.setExternalId(senderExternalId);
        sameSender.setEmail(senderEmail);
        RoleEntity role = new RoleEntity(new Role(ADMIN.getId(), RoleName.ADMIN, "Admin Role"));
        sameSender.addServiceRole(new ServiceRoleEntity(service, role));

        when(mockServiceDao.findByExternalId(serviceExternalId)).thenReturn(Optional.of(service));
        when(linksConfig.getSelfserviceInvitesUrl()).thenReturn("http://selfservice/invites");

        String inviteCode = randomUuid();
        InviteEntity anInvite = anInvite(email, inviteCode, "otpKey", sameSender, service, role);
        when(mockInviteDao.findByEmail(email)).thenReturn(List.of(anInvite));
        return anInvite;
    }

    private InviteEntity mockInviteSuccessForNonExistingUserNonExistingInvite() {

        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);
        service.setExternalId(serviceExternalId);

        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());
        when(mockInviteDao.findByEmail(email)).thenReturn(emptyList());
        when(mockServiceDao.findByExternalId(serviceExternalId)).thenReturn(Optional.of(service));
        when(mockRoleDao.findByRoleName(viewRole.getRoleName())).thenReturn(Optional.of(new RoleEntity(viewRole)));

        UserEntity senderUser = new UserEntity();
        senderUser.setExternalId(senderExternalId);
        senderUser.setEmail(senderEmail);
        RoleEntity role = new RoleEntity(new Role(ADMIN.getId(), RoleName.ADMIN, "Admin Role"));
        senderUser.addServiceRole(new ServiceRoleEntity(service, role));
        when(mockUserDao.findByExternalId(senderExternalId)).thenReturn(Optional.of(senderUser));

        String inviteCode = "code";
        InviteEntity anInvite = anInvite(email, inviteCode, "otpKey", senderUser, service, role);
        
        return anInvite;
    }

    private InviteEntity anInvite(String email, String code, String otpKey, UserEntity userEntity, ServiceEntity serviceEntity, RoleEntity roleEntity) {
        InviteEntity inviteEntity = new InviteEntity(email, code, otpKey, roleEntity);
        inviteEntity.setSender(userEntity);
        inviteEntity.setService(serviceEntity);
        return inviteEntity;
    }

    private User aUser(String email) {
        Service service = Service.from(serviceId, serviceExternalId, new ServiceName(Service.DEFAULT_NAME_VALUE));
        ServiceRole serviceRole = ServiceRole.from(service, new Role(ADMIN.getId(), RoleName.ADMIN, "Administrator"));
        return User.from(randomInt(), randomUuid(), "random-password", email,
                "784rh", "8948924", Collections.singletonList(serviceRole), null,
                SecondFactorMethod.SMS, null, null, null);
    }

}
