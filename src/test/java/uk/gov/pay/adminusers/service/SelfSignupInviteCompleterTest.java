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
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.GatewayAccountIdEntity;
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

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.model.Role.role;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

@ExtendWith(MockitoExtension.class)
public class SelfSignupInviteCompleterTest {
    @Mock
    private ServiceDao mockServiceDao;
    @Mock
    private UserDao mockUserDao;
    @Mock
    private InviteDao mockInviteDao;

    private InviteCompleter selfSignupInviteCompleter;
    private ArgumentCaptor<UserEntity> expectedInvitedUser = ArgumentCaptor.forClass(UserEntity.class);
    private ArgumentCaptor<InviteEntity> expectedInvite = ArgumentCaptor.forClass(InviteEntity.class);
    private ArgumentCaptor<ServiceEntity> expectedService = ArgumentCaptor.forClass(ServiceEntity.class);

    private String otpKey = "otpKey";
    private String inviteCode = "code";
    private String senderEmail = "sender@example.com";
    private String email = "invited@example.com";
    private int serviceId = 1;
    private String senderExternalId = "12345";
    private String baseUrl = "http://localhost";

    @BeforeEach
    public void setUp() {
        selfSignupInviteCompleter = new SelfSignupInviteCompleter(
                mockInviteDao,
                mockUserDao,
                mockServiceDao,
                new LinksBuilder(baseUrl)
        );
    }

    @Test
    public void shouldCreateServiceAndUser_withGatewayAccounts_whenPassedValidServiceInviteCode() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.SERVICE);
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());

        InviteCompleteRequest data = new InviteCompleteRequest();
        data.setGatewayAccountIds(asList("1", "2"));
        InviteCompleteResponse inviteResponse = selfSignupInviteCompleter.withData(data).complete(anInvite);

        verify(mockServiceDao).persist(expectedService.capture());
        verify(mockUserDao).merge(expectedInvitedUser.capture());
        verify(mockInviteDao).merge(expectedInvite.capture());

        ServiceEntity serviceEntity = expectedService.getValue();
        assertThat(serviceEntity.getGatewayAccountIds().stream()
                .map(GatewayAccountIdEntity::getGatewayAccountId)
                .collect(toUnmodifiableList()), hasItems("2", "1"));
        assertThat(serviceEntity.getServiceNames().get(SupportedLanguage.ENGLISH).getName(), is(Service.DEFAULT_NAME_VALUE));
        assertThat(serviceEntity.isRedirectToServiceImmediatelyOnTerminalState(), is(false));
        assertThat(serviceEntity.isCollectBillingAddress(), is(true));
        assertThat(serviceEntity.getDefaultBillingAddressCountry(), is("GB"));

        assertThat(inviteResponse.getInvite().isDisabled(), is(true));
        assertThat(inviteResponse.getInvite().getLinks().size(), is(1));
        assertThat(inviteResponse.getInvite().getLinks().get(0).getRel(), is(Link.Rel.USER));
        assertThat(inviteResponse.getInvite().getLinks().get(0).getHref(), matchesPattern("^" + baseUrl + "/v1/api/users/[0-9a-z]{32}$"));
    }

    @Test
    public void shouldCreateServiceAndUser_withoutGatewayAccounts_whenPassedValidServiceInviteCode() {
        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.SERVICE);
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());

        InviteCompleteResponse inviteResponse = selfSignupInviteCompleter.withData(new InviteCompleteRequest()).complete(anInvite);

        verify(mockServiceDao).persist(expectedService.capture());
        verify(mockUserDao).merge(expectedInvitedUser.capture());
        verify(mockInviteDao).merge(expectedInvite.capture());

        assertThat(expectedService.getValue().getGatewayAccountIds().isEmpty(), is(true));

        ServiceEntity serviceEntity = expectedService.getValue();
        assertThat(serviceEntity.getGatewayAccountIds().isEmpty(), is(true));
        assertThat(serviceEntity.getServiceNames().get(SupportedLanguage.ENGLISH).getName(), is(Service.DEFAULT_NAME_VALUE));
        assertThat(serviceEntity.isRedirectToServiceImmediatelyOnTerminalState(), is(false));
        assertThat(serviceEntity.isCollectBillingAddress(), is(true));
        assertThat(serviceEntity.getDefaultBillingAddressCountry(), is("GB"));

        assertThat(inviteResponse.getInvite().isDisabled(), is(true));
        assertThat(inviteResponse.getInvite().getLinks().size(), is(1));
        assertThat(inviteResponse.getInvite().getLinks().get(0).getRel(), is(Link.Rel.USER));
        assertThat(inviteResponse.getInvite().getLinks().get(0).getHref(), matchesPattern("^" + baseUrl + "/v1/api/users/[0-9a-z]{32}$"));
    }

    @Test
    public void shouldThrowConflict_whenPassedInviteEmailAlreadyHasARegisteredUser() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.SERVICE);

        when(mockUserDao.findByEmail(anInvite.getEmail())).thenReturn(Optional.of(mock(UserEntity.class)));

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> selfSignupInviteCompleter.complete(anInvite));
        assertThat(exception.getMessage(), is("HTTP 409 Conflict"));
    }

    @Test
    public void shouldThrowEmailExistsException_whenPassedInviteCodeWhichIsDisabled() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.SERVICE);
        anInvite.setDisabled(true);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> selfSignupInviteCompleter.complete(anInvite));
        assertThat(exception.getMessage(), is("HTTP 410 Gone"));
    }

    @Test
    public void shouldThrowEmailExistsException_whenPassedInviteCodeWhichIsExpired() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.SERVICE);
        anInvite.setExpiryDate(ZonedDateTime.now().minusDays(1));

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> selfSignupInviteCompleter.complete(anInvite));
        assertThat(exception.getMessage(), is("HTTP 410 Gone"));
    }

    @Test
    public void shouldError_whenTryingToCreateServiceAndService_ifInviteIsOfUserType() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.USER);
        
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> selfSignupInviteCompleter.complete(anInvite));
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
