package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.pay.adminusers.model.InviteCompleteRequest;
import uk.gov.pay.adminusers.model.InviteCompleteResponse;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.*;

import javax.ws.rs.WebApplicationException;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static uk.gov.pay.adminusers.model.Role.role;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

@RunWith(MockitoJUnitRunner.class)
public class ServiceInviteCompleterTest {
    @Mock
    private RoleDao mockRoleDao;
    @Mock
    private ServiceDao mockServiceDao;
    @Mock
    private UserDao mockUserDao;
    @Mock
    private InviteDao mockInviteDao;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private InviteCompleter serviceInviteCompleter;
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
        serviceInviteCompleter = new ServiceInviteCompleter(
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
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        when(mockRoleDao.findByRoleName(roleName)).thenReturn(Optional.of(new RoleEntity()));

        InviteCompleteRequest data = new InviteCompleteRequest();
        data.setGatewayAccountIds(asList("1", "2"));
        InviteCompleteResponse inviteResponse = serviceInviteCompleter.withData(data).complete(anInvite.getCode()).get();

        verify(mockServiceDao).persist(expectedService.capture());
        verify(mockUserDao).merge(expectedInvitedUser.capture());
        verify(mockInviteDao).merge(expectedInvite.capture());

        assertThat(expectedService.getValue().getGatewayAccountIds().stream().map(gaie -> gaie.getGatewayAccountId()).collect(toList()), hasItems("2", "1"));
        assertThat(inviteResponse.getInvite().isDisabled(), is(true));
        assertThat(inviteResponse.getInvite().getLinks().size(), is(1));
        assertThat(inviteResponse.getInvite().getLinks().get(0).getRel().toString(), is("user"));
        assertThat(inviteResponse.getInvite().getLinks().get(0).getHref(), matchesPattern("^" + baseUrl + "/v1/api/users/[0-9a-z]{32}$"));
    }

    @Test
    public void shouldCreateServiceAndUser_withoutGatewayAccounts_whenPassedValidServiceInviteCode() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.SERVICE);
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        when(mockRoleDao.findByRoleName(roleName)).thenReturn(Optional.of(new RoleEntity()));

        InviteCompleteResponse inviteResponse = serviceInviteCompleter.withData(new InviteCompleteRequest()).complete(anInvite.getCode()).get();

        verify(mockServiceDao).persist(expectedService.capture());
        verify(mockUserDao).merge(expectedInvitedUser.capture());
        verify(mockInviteDao).merge(expectedInvite.capture());

        assertThat(expectedService.getValue().getGatewayAccountIds().isEmpty(), is(true));

        assertThat(inviteResponse.getInvite().isDisabled(), is(true));
        assertThat(inviteResponse.getInvite().getLinks().size(), is(1));
        assertThat(inviteResponse.getInvite().getLinks().get(0).getRel().toString(), is("user"));
        assertThat(inviteResponse.getInvite().getLinks().get(0).getHref(), matchesPattern("^" + baseUrl + "/v1/api/users/[0-9a-z]{32}$"));
    }

    @Test
    public void shouldThrowConflict_whenPassedInviteEmailAlreadyHasARegisteredUser() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.SERVICE);

        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        when(mockUserDao.findByEmail(anInvite.getEmail())).thenReturn(Optional.of(mock(UserEntity.class)));

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 409 Conflict");
        serviceInviteCompleter.complete(anInvite.getCode());
    }

    @Test
    public void shouldThrowEmailExistsException_whenPassedInviteCodeWhichIsDisabled() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.SERVICE);
        anInvite.setDisabled(true);

        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 410 Gone");
        serviceInviteCompleter.complete(anInvite.getCode());
    }

    @Test
    public void shouldThrowEmailExistsException_whenPassedInviteCodeWhichIsExpired() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.SERVICE);
        anInvite.setExpiryDate(ZonedDateTime.now().minusDays(1));

        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 410 Gone");
        serviceInviteCompleter.complete(anInvite.getCode());
    }

    @Test
    public void shouldError_whenTryingToCreateServiceAndService_ifInviteIsOfUserType() throws Exception {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        InviteEntity anInvite = createInvite();
        anInvite.setType(InviteType.USER);

        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 500 Internal Server Error");
        serviceInviteCompleter.complete(anInvite.getCode());
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

}
