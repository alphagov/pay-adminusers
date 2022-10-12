package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.model.InviteCompleteRequest;
import uk.gov.pay.adminusers.model.InviteCompleteResponse;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.model.Link;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import javax.ws.rs.WebApplicationException;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.model.Role.role;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

@ExtendWith(MockitoExtension.class)
public class NewUserExistingServiceInviteCompleterTest {

    @Mock
    private UserDao mockUserDao;
    @Mock
    private InviteDao mockInviteDao;

    private InviteCompleter newUserExistingServiceInviteCompleter;
    private final ArgumentCaptor<UserEntity> expectedInvitedUser = ArgumentCaptor.forClass(UserEntity.class);
    private final ArgumentCaptor<InviteEntity> expectedInvite = ArgumentCaptor.forClass(InviteEntity.class);

    private final String otpKey = "otpKey";
    private final String inviteCode = "code";
    private final String senderEmail = "sender@example.com";
    private final String email = "invited@example.com";
    private final int serviceId = 1;
    private final String senderExternalId = "12345";
    private final String baseUrl = "http://localhost";

    @BeforeEach
    public void setUp() {
        newUserExistingServiceInviteCompleter = new NewUserExistingServiceInviteCompleter(
                mockInviteDao,
                mockUserDao,
                new LinksBuilder(baseUrl)
        );
    }

    @Test
    public void shouldCreateUserAndAssignThemToService_whenPassedValidServiceInviteCode() {
        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.NEW_USER_INVITED_TO_EXISTING_SERVICE);
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());

        InviteCompleteResponse inviteResponse = newUserExistingServiceInviteCompleter.withData(new InviteCompleteRequest()).complete(anInvite);

        verify(mockUserDao).persist(expectedInvitedUser.capture());
        verify(mockInviteDao).merge(expectedInvite.capture());

        UserEntity user = expectedInvitedUser.getValue();
        assertThat(inviteResponse.getInvite().isDisabled(), is(true));
        assertThat(inviteResponse.getInvite().getLinks().size(), is(1));
        assertThat(inviteResponse.getInvite().getLinks().get(0).getRel(), is(Link.Rel.USER));
        assertThat(inviteResponse.getInvite().getLinks().get(0).getHref(), matchesPattern("^" + baseUrl + "/v1/api/users/[0-9a-z]{32}$"));
        assertThat(inviteResponse.getUserExternalId(), is(user.getExternalId()));

        assertThat(user.getServicesRoles().size(), is(1));
        assertThat(user.getServicesRoles().get(0).getRole(), is(anInvite.getRole()));
        assertThat(user.getServicesRoles().get(0).getService(), is(anInvite.getService()));
        assertThat(user.getServicesRoles().get(0).getUser(), is(user));
    }

    @Test
    public void shouldThrowConflict_whenPassedInviteEmailAlreadyHasARegisteredUser() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.NEW_USER_INVITED_TO_EXISTING_SERVICE);

        when(mockUserDao.findByEmail(anInvite.getEmail())).thenReturn(Optional.of(mock(UserEntity.class)));

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> newUserExistingServiceInviteCompleter.complete(anInvite));
        assertThat(exception.getMessage(), is("HTTP 409 Conflict"));
    }

    @Test
    public void shouldThrowEmailExistsException_whenPassedInviteCodeWhichIsDisabled() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.NEW_USER_INVITED_TO_EXISTING_SERVICE);
        anInvite.setDisabled(true);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> newUserExistingServiceInviteCompleter.complete(anInvite));
        assertThat(exception.getMessage(), is("HTTP 410 Gone"));
    }

    @Test
    public void shouldThrowEmailExistsException_whenPassedInviteCodeWhichIsExpired() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.NEW_USER_INVITED_TO_EXISTING_SERVICE);
        anInvite.setExpiryDate(ZonedDateTime.now().minusDays(1));

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> newUserExistingServiceInviteCompleter.complete(anInvite));
        assertThat(exception.getMessage(), is("HTTP 410 Gone"));
    }

    @Test
    public void shouldError_whenTryingToCreateServiceAndService_ifInviteIsOfExistingUserType() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.EXISTING_USER_INVITED_TO_EXISTING_SERVICE);

        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> newUserExistingServiceInviteCompleter.complete(anInvite));
        assertThat(exception.getMessage(), is("HTTP 500 Internal Server Error"));
    }

    @Test
    public void shouldError_whenTryingToCreateServiceAndService_ifInviteIsOfSelfSignupType() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.NEW_USER_AND_NEW_SERVICE_SELF_SIGNUP);

        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> newUserExistingServiceInviteCompleter.complete(anInvite));
        assertThat(exception.getMessage(), is("HTTP 500 Internal Server Error"));
    }

    private InviteEntity createInvite() {
        ServiceEntity service = new ServiceEntity();
        service.addOrUpdateServiceName(ServiceNameEntity.from(SupportedLanguage.ENGLISH, Service.DEFAULT_NAME_VALUE));
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

}
