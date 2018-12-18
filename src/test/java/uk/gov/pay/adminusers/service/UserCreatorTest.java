package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.model.CreateUserRequest;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.ws.rs.WebApplicationException;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class UserCreatorTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();
    @Mock
    private UserDao mockUserDao;
    @Mock
    private ServiceDao mockServiceDao;
    @Mock
    private RoleDao mockRoleDao;
    @Mock
    private PasswordHasher mockPasswordHasher;
    private final LinksBuilder linksBuilder = new LinksBuilder("http://localhost");
    final ArgumentCaptor<UserEntity> expectedUser = ArgumentCaptor.forClass(UserEntity.class);

    private UserCreator userCreator;

    @Before
    public void before() {
        userCreator = new UserCreator(mockUserDao, mockRoleDao, mockServiceDao, mockPasswordHasher, linksBuilder);
    }

    @Test
    public void shouldSaveAndReturnUser_forValidUserCreationRequest() {
        String validRole = "validRole";
        when(mockRoleDao.findByRoleName(validRole)).thenReturn(Optional.of(mock(RoleEntity.class)));
        CreateUserRequest request = CreateUserRequest.from("email@example.com", "password", "email@example.com", null, null, "otpKey", "3745838475", null);

        User user = userCreator.doCreate(request, validRole);

        verify(mockUserDao).persist(expectedUser.capture());
        assertThat(expectedUser.getValue().getEmail(), is("email@example.com"));
        assertThat(user.getEmail(), is("email@example.com"));
        assertThat(user.getSecondFactor(), is(SecondFactorMethod.SMS));
        assertThat(user.getServiceRoles().size(), is(0));
    }

    @Test
    public void shouldSaveAndReturnUser_forValidUserCreationRequest_withGatewayAccountIds() {
        String validRole = "validRole";
        when(mockRoleDao.findByRoleName(validRole)).thenReturn(Optional.of(mock(RoleEntity.class)));
        CreateUserRequest request = CreateUserRequest.from("email@example.com", "password", "email@example.com", asList("1", "2"), null, "otpKey", "3745838475", null);
        User user = userCreator.doCreate(request, validRole);

        verify(mockUserDao).persist(expectedUser.capture());
        assertThat(expectedUser.getValue().getEmail(), is("email@example.com"));
        assertThat(user.getEmail(), is("email@example.com"));
        assertThat(user.getSecondFactor(), is(SecondFactorMethod.SMS));
        assertThat(user.getServiceRoles().size(), is(1));
        
        Service service = user.getServiceRoles().get(0).getService();
        assertThat(service.getName(), is (Service.DEFAULT_NAME_VALUE));
        assertThat(service.getGatewayAccountIds(), is(asList("1", "2")));
        assertThat(service.isRedirectToServiceImmediatelyOnTerminalState(), is(false));
        assertThat(service.isCollectBillingAddress(), is(true));
    }

    @Test
    public void shouldSaveAndReturnUser_forValidUserCreationRequest_withServiceRoles() {
        String validRole = "validRole";
        when(mockRoleDao.findByRoleName(validRole)).thenReturn(Optional.of(mock(RoleEntity.class)));
        CreateUserRequest request = CreateUserRequest.from("email@example.com", "password", "email@example.com", null, asList("ext-id-1", "ext-id-2"), "otpKey", "3745838475", null);
        when(mockServiceDao.findByExternalId("ext-id-1")).thenReturn(Optional.of(mock(ServiceEntity.class)));
        when(mockServiceDao.findByExternalId("ext-id-2")).thenReturn(Optional.of(mock(ServiceEntity.class)));
        User user = userCreator.doCreate(request, validRole);

        verify(mockUserDao).persist(expectedUser.capture());
        assertThat(expectedUser.getValue().getEmail(), is("email@example.com"));
        assertThat(user.getEmail(), is("email@example.com"));
        assertThat(user.getSecondFactor(), is(SecondFactorMethod.SMS));
        assertThat(user.getServiceRoles().size(), is(2));
    }

    @Test
    public void shouldSaveAndReturnUser_forValidUserCreationRequest_withServiceRoles_evenIfSomeExternalIdsMissing() {
        String validRole = "validRole";
        when(mockRoleDao.findByRoleName(validRole)).thenReturn(Optional.of(mock(RoleEntity.class)));
        CreateUserRequest request = CreateUserRequest.from("email@example.com", "password", "email@example.com", null, asList("ext-id-1", "ext-id-2"), "otpKey", "3745838475", null);
        when(mockServiceDao.findByExternalId("ext-id-1")).thenReturn(Optional.of(mock(ServiceEntity.class)));
        when(mockServiceDao.findByExternalId("ext-id-2")).thenReturn(Optional.empty());
        User user = userCreator.doCreate(request, validRole);

        verify(mockUserDao).persist(expectedUser.capture());
        assertThat(expectedUser.getValue().getEmail(), is("email@example.com"));
        assertThat(user.getEmail(), is("email@example.com"));
        assertThat(user.getSecondFactor(), is(SecondFactorMethod.SMS));
        assertThat(user.getServiceRoles().size(), is(1));
    }

    @Test
    public void shouldError_ifRoleIsInvalid() {
        String validRole = "inValidRole";
        when(mockRoleDao.findByRoleName(validRole)).thenReturn(Optional.empty());
        CreateUserRequest request = CreateUserRequest.from("email@example.com", "password", "email@example.com", null, null, "otpKey", "3745838475", null);

        expectedException.expect(WebApplicationException.class);
        expectedException.expectMessage("HTTP 400 Bad Request");
        userCreator.doCreate(request, validRole);
    }
}
