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
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

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

    private static final String EXISTING_USER_EXTERNAL_ID = "7d19aff33f8948deb97ed16b2912dcd3";
    private static final String NON_EXISTENT_USER_EXTERNAL_ID = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    @Before
    public void before() throws Exception {
        serviceRoleUpdater = new ServiceRoleUpdater(userDao, serviceDao, roleDao, new LinksBuilder("http://localhost"));
    }

    @Test
    public void shouldReturnEmpty_ifUserNotFound_whenUpdatingServiceRole() throws Exception {
        when(userDao.findByExternalId(NON_EXISTENT_USER_EXTERNAL_ID)).thenReturn(Optional.empty());

        Optional<User> userOptional = serviceRoleUpdater.doUpdate(NON_EXISTENT_USER_EXTERNAL_ID, randomInt(), "randomRole");
        assertFalse(userOptional.isPresent());
    }

    @Test
    public void shouldError_ifRoleNotFound_whenUpdatingServiceRole() throws Exception {
        String randomRole = "randomRole";
        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID))));
        when(roleDao.findByRoleName(randomRole)).thenReturn(Optional.empty());

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 400 Bad Request");
        serviceRoleUpdater.doUpdate(EXISTING_USER_EXTERNAL_ID, randomInt(), randomRole);
    }

    @Test
    public void shouldError_ifServiceNotBelongToUser_whenUpdatingServiceRole() throws Exception {
        String role = "a-role";
        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID))));
        when(roleDao.findByRoleName(role)).thenReturn(Optional.of(new RoleEntity(aRole(1, role))));

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 409 Conflict");
        serviceRoleUpdater.doUpdate(EXISTING_USER_EXTERNAL_ID, randomInt(), role);
    }

    @Test
    public void shouldError_ifCountOfServiceAdminsLessThan1_whenUpdatingServiceRole() throws Exception {
        String role = "a-role";
        Integer serviceId = 1;

        UserEntity userEntity = UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID));
        RoleEntity roleEntity = new RoleEntity(aRole(10, role)); //non admin
        ServiceEntity serviceEntity = new ServiceEntity(asList("1"));
        serviceEntity.setId(serviceId);
        userEntity.setServiceRole(new ServiceRoleEntity(serviceEntity, roleEntity));

        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(userEntity));
        when(roleDao.findByRoleName(role)).thenReturn(Optional.of(roleEntity));
        when(serviceDao.countOfRolesForService(serviceId, ADMIN.getId())).thenReturn(1L);

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 412 Precondition Failed");
        serviceRoleUpdater.doUpdate(EXISTING_USER_EXTERNAL_ID, serviceId, role);
    }

    @Test
    public void shouldReturnUpdatedUser_whenUpdatingServiceRoleSuccess() throws Exception {
        String role = "a-role";
        Integer serviceId = 1;

        UserEntity userEntity = UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID));
        RoleEntity roleEntity = new RoleEntity(aRole(10, role)); //non admin
        ServiceEntity serviceEntity = new ServiceEntity(asList("1"));
        serviceEntity.setId(serviceId);
        userEntity.setServiceRole(new ServiceRoleEntity(serviceEntity, roleEntity));

        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(userEntity));
        when(roleDao.findByRoleName(role)).thenReturn(Optional.of(roleEntity));
        when(serviceDao.countOfRolesForService(serviceId, ADMIN.getId())).thenReturn(2L);

        Optional<User> userOptional = serviceRoleUpdater.doUpdate(EXISTING_USER_EXTERNAL_ID, serviceId, role);
        assertTrue(userOptional.isPresent());
        assertThat(userOptional.get().getRole().getId(), is(10));
    }

    private Role aRole(int roleId, String roleName) {
        return Role.role(roleId, roleName, roleName + "-description");
    }

    private User aUser(String externalId) {
        return User.from(randomInt(), externalId, "random-name", "random-password", "random@example.com", asList("1"), newArrayList(), "784rh", "8948924");
    }
}
