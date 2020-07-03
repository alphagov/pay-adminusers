package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceName;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.ws.rs.WebApplicationException;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRoleCreatorTest {
    @Mock
    private UserDao userDao;
    @Mock
    private RoleDao roleDao;
    @Mock
    private ServiceDao serviceDao;

    private ServiceRoleCreator serviceRoleCreator;

    private static final String EXISTING_USER_EXTERNAL_ID = "7d19aff33f8948deb97ed16b2912dcd3";
    private static final String EXISTING_SERVICE_EXTERNAL_ID = "8374rgw88934r98c9io";
    private static final String EXISTING_ROLE_NAME = "admin";

    @Before
    public void before() {
        serviceRoleCreator = new ServiceRoleCreator(userDao, serviceDao, roleDao, new LinksBuilder("http://localhost"));
    }

    @Test
    public void shouldSuccess_whenAssignANewServiceRole() {
        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID))));
        when(serviceDao.findByExternalId(EXISTING_SERVICE_EXTERNAL_ID)).thenReturn(Optional.of(ServiceEntity.from(aService(EXISTING_SERVICE_EXTERNAL_ID))));
        when(roleDao.findByRoleName(EXISTING_ROLE_NAME)).thenReturn(Optional.of(new RoleEntity(aRole(1, EXISTING_ROLE_NAME))));

        Optional<User> userOptional = serviceRoleCreator.doCreate(EXISTING_USER_EXTERNAL_ID, EXISTING_SERVICE_EXTERNAL_ID, EXISTING_ROLE_NAME);

        assertTrue(userOptional.isPresent());

        User user = userOptional.get();
        assertThat(user.getServiceRoles().size(), is(1));
        assertThat(user.getServiceRoles().get(0).getRole().getName(), is(EXISTING_ROLE_NAME));
        assertThat(user.getServiceRoles().get(0).getService().getExternalId(), is(EXISTING_SERVICE_EXTERNAL_ID));
    }

    @Test
    public void shouldReturnEmpty_whenAssignANewServiceRole_ifUserNotFound() {
        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.empty());

        Optional<User> userOptional = serviceRoleCreator.doCreate(EXISTING_USER_EXTERNAL_ID, EXISTING_SERVICE_EXTERNAL_ID, EXISTING_ROLE_NAME);

        assertFalse(userOptional.isPresent());
    }

    @Test
    public void shouldError400_whenAssignANewServiceRole_ifServiceNotFound() {
        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID))));
        when(serviceDao.findByExternalId(EXISTING_SERVICE_EXTERNAL_ID)).thenReturn(Optional.empty());

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> serviceRoleCreator.doCreate(EXISTING_USER_EXTERNAL_ID, EXISTING_SERVICE_EXTERNAL_ID, EXISTING_ROLE_NAME));
        assertThat(exception.getMessage(), is("HTTP 400 Bad Request"));
    }

    @Test
    public void shouldError400_whenAssignANewServiceRole_ifRoleNotFound() {
        ServiceEntity serviceEntity = ServiceEntity.from(aService(EXISTING_SERVICE_EXTERNAL_ID));
        UserEntity userEntity = UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID));
        RoleEntity roleEntity = new RoleEntity(aRole(1, EXISTING_ROLE_NAME));

        userEntity.addServiceRole(new ServiceRoleEntity(serviceEntity, roleEntity));

        when(serviceDao.findByExternalId(EXISTING_SERVICE_EXTERNAL_ID)).thenReturn(Optional.of(serviceEntity));
        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(userEntity));
        when(roleDao.findByRoleName(EXISTING_ROLE_NAME)).thenReturn(Optional.of(roleEntity));

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> serviceRoleCreator.doCreate(EXISTING_USER_EXTERNAL_ID, EXISTING_SERVICE_EXTERNAL_ID, EXISTING_ROLE_NAME));
        assertThat(exception.getMessage(), is("HTTP 409 Conflict"));
    }

    @Test
    public void shouldError409_whenAssignANewServiceRole_ifRoleForServiceAlreadyExists() {
        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID))));
        when(serviceDao.findByExternalId(EXISTING_SERVICE_EXTERNAL_ID)).thenReturn(Optional.of(ServiceEntity.from(aService(EXISTING_SERVICE_EXTERNAL_ID))));
        when(roleDao.findByRoleName(EXISTING_ROLE_NAME)).thenReturn(Optional.empty());

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> serviceRoleCreator.doCreate(EXISTING_USER_EXTERNAL_ID, EXISTING_SERVICE_EXTERNAL_ID, EXISTING_ROLE_NAME));
        assertThat(exception.getMessage(), is("HTTP 400 Bad Request"));
    }

    private Service aService(String serviceExternalId) {
        return Service.from(randomInt(), serviceExternalId, new ServiceName("random-service"));
    }

    private Role aRole(int roleId, String roleName) {
        return Role.role(roleId, roleName, roleName + "-description");
    }

    private User aUser(String externalId) {
        return User.from(randomInt(), externalId, "random-name", "random-password", "random@example.com",
                "784rh", "8948924", emptyList(), null, SecondFactorMethod.SMS,
                null, null, null);
    }
}
