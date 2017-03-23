package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.pay.adminusers.model.Role;
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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.model.Role.ROLE_ADMIN_ID;


@RunWith(MockitoJUnitRunner.class)
public class ServiceRoleUpdaterTest {

    @Mock
    private UserDao userDao;
    @Mock
    private RoleDao roleDao;
    @Mock
    private ServiceDao serviceDao;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ServiceRoleUpdater serviceRoleUpdater;

    @Before
    public void before() throws Exception {
        serviceRoleUpdater = new ServiceRoleUpdater(userDao, serviceDao, roleDao, new LinksBuilder("http://localhost"));
    }

    @Test
    public void shouldReturnEmpty_ifUserNotFound_whenUpdatingServiceRole() throws Exception {
        String username = "non-existent";
        when(userDao.findByUsername(username)).thenReturn(Optional.empty());

        Optional<User> userOptional = serviceRoleUpdater.doUpdate(username, randomInt(), "randomRole");
        assertFalse(userOptional.isPresent());
    }

    @Test
    public void shouldError_ifRoleNotFound_whenUpdatingServiceRole() throws Exception {
        String username = "existing-user";
        String randomRole = "randomRole";
        when(userDao.findByUsername(username)).thenReturn(Optional.of(UserEntity.from(aUser(username))));
        when(roleDao.findByRoleName(randomRole)).thenReturn(Optional.empty());

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 400 Bad Request");
        serviceRoleUpdater.doUpdate(username, randomInt(), randomRole);
    }

    @Test
    public void shouldError_ifServiceNotBelongToUser_whenUpdatingServiceRole() throws Exception {
        String username = "existing-user";
        String role = "a-role";
        when(userDao.findByUsername(username)).thenReturn(Optional.of(UserEntity.from(aUser(username))));
        when(roleDao.findByRoleName(role)).thenReturn(Optional.of(new RoleEntity(aRole(1, role))));

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 409 Conflict");
        serviceRoleUpdater.doUpdate(username, randomInt(), role);
    }

    @Test
    public void shouldError_ifCountOfServiceAdminsLessThan1_whenUpdatingServiceRole() throws Exception {
        String username = "existing-user";
        String role = "a-role";
        Integer serviceId = 1;

        UserEntity userEntity = UserEntity.from(aUser(username));
        RoleEntity roleEntity = new RoleEntity(aRole(10, role)); //non admin
        ServiceEntity serviceEntity = new ServiceEntity(asList("1"));
        serviceEntity.setId(serviceId);
        userEntity.setServiceRole(new ServiceRoleEntity(serviceEntity, roleEntity));

        when(userDao.findByUsername(username)).thenReturn(Optional.of(userEntity));
        when(roleDao.findByRoleName(role)).thenReturn(Optional.of(roleEntity));
        when(serviceDao.countOfRolesForService(serviceId,ROLE_ADMIN_ID)).thenReturn(1l);

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 412 Precondition Failed");
        serviceRoleUpdater.doUpdate(username, serviceId, role);
    }

    @Test
    public void shouldReturnUpdatedUser_whenUpdatingServiceRoleSuccess() throws Exception {
        String username = "existing-user";
        String role = "a-role";
        Integer serviceId = 1;

        UserEntity userEntity = UserEntity.from(aUser(username));
        RoleEntity roleEntity = new RoleEntity(aRole(10, role)); //non admin
        ServiceEntity serviceEntity = new ServiceEntity(asList("1"));
        serviceEntity.setId(serviceId);
        userEntity.setServiceRole(new ServiceRoleEntity(serviceEntity, roleEntity));

        when(userDao.findByUsername(username)).thenReturn(Optional.of(userEntity));
        when(roleDao.findByRoleName(role)).thenReturn(Optional.of(roleEntity));
        when(serviceDao.countOfRolesForService(serviceId,ROLE_ADMIN_ID)).thenReturn(2l);

        Optional<User> userOptional = serviceRoleUpdater.doUpdate(username, serviceId, role);
        assertTrue(userOptional.isPresent());
        assertThat(userOptional.get().getRole().getId(),is(10));
    }

    private Role aRole(int roleId, String roleName) {
        return Role.role(roleId, roleName, roleName + "-description");
    }

    private User aUser(String username) {
        return User.from(randomInt(), username, "random-password", "random@email.com", asList("1"), newArrayList(), "784rh", "8948924");
    }
}
