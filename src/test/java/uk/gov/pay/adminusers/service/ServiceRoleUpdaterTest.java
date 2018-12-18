package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
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
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
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
    public final ExpectedException thrown = ExpectedException.none();

    private ServiceRoleUpdater serviceRoleUpdater;

    private static final String EXISTING_USER_EXTERNAL_ID = "7d19aff33f8948deb97ed16b2912dcd3";
    private static final String NON_EXISTENT_USER_EXTERNAL_ID = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    @Before
    public void before() {
        serviceRoleUpdater = new ServiceRoleUpdater(userDao, serviceDao, roleDao, new LinksBuilder("http://localhost"));
    }

    @Test
    public void shouldReturnEmpty_ifUserNotFound_whenUpdatingServiceRole() {
        when(userDao.findByExternalId(NON_EXISTENT_USER_EXTERNAL_ID)).thenReturn(Optional.empty());

        Optional<User> userOptional = serviceRoleUpdater.doUpdate(NON_EXISTENT_USER_EXTERNAL_ID, randomUuid(), "randomRole");
        assertFalse(userOptional.isPresent());
    }

    @Test
    public void shouldError_ifRoleNotFound_whenUpdatingServiceRole() {
        String randomRole = "randomRole";
        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID))));
        when(roleDao.findByRoleName(randomRole)).thenReturn(Optional.empty());

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 400 Bad Request");
        serviceRoleUpdater.doUpdate(EXISTING_USER_EXTERNAL_ID, randomUuid(), randomRole);
    }

    @Test
    public void shouldError_ifServiceNotBelongToUser_whenUpdatingServiceRole() {
        String role = "a-role";
        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID))));
        when(roleDao.findByRoleName(role)).thenReturn(Optional.of(new RoleEntity(aRole(1, role))));

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 409 Conflict");
        serviceRoleUpdater.doUpdate(EXISTING_USER_EXTERNAL_ID, randomUuid(), role);
    }

    @Test
    public void shouldError_ifCountOfServiceAdminsLessThan1_whenUpdatingServiceRole() {
        String role = "a-role";
        String serviceExternalId = "sxrdctfvygbuhinj";

        UserEntity userEntity = UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID));

        RoleEntity targetRoleEntity = new RoleEntity(aRole(10, role));
        RoleEntity currentRoleEntity = new RoleEntity(aRole(ADMIN.getId(), "admin"));

        ServiceEntity serviceEntity = new ServiceEntity(singletonList("1"));
        serviceEntity.setExternalId(serviceExternalId);

        userEntity.addServiceRole(new ServiceRoleEntity(serviceEntity, currentRoleEntity));

        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(userEntity));
        when(roleDao.findByRoleName(role)).thenReturn(Optional.of(targetRoleEntity));
        when(serviceDao.countOfUsersWithRoleForService(serviceExternalId, ADMIN.getId())).thenReturn(1L);

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 412 Precondition Failed");
        serviceRoleUpdater.doUpdate(EXISTING_USER_EXTERNAL_ID, serviceExternalId, role);
    }

    @Test
    public void shouldReturnUpdatedUser_whenUpdatingServiceRoleSuccess() {
        String role = "another-non-admin-role";
        String serviceExternalId = "sxrdctfvygbuhinj";

        UserEntity userEntity = UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID));

        RoleEntity targetRoleEntity = new RoleEntity(aRole(10, role));
        RoleEntity currentRoleEntity = new RoleEntity(aRole(9, "non-admin-role"));

        ServiceEntity serviceEntity = new ServiceEntity(singletonList("1"));
        serviceEntity.setExternalId(serviceExternalId);

        userEntity.addServiceRole(new ServiceRoleEntity(serviceEntity, currentRoleEntity));

        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(userEntity));
        when(roleDao.findByRoleName(role)).thenReturn(Optional.of(targetRoleEntity));

        Optional<User> userOptional = serviceRoleUpdater.doUpdate(EXISTING_USER_EXTERNAL_ID, serviceExternalId, role);
        assertTrue(userOptional.isPresent());
        assertThat(userOptional.get().getRole().getId(), is(10));
    }

    @Test
    public void shouldReturnUpdatedUser_whenDowngradingAdminWhenEnoughAdminsSuccess() {
        String role = "non-admin-role";
        String serviceExternalId = "sxrdctfvygbuhinj";

        UserEntity userEntity = UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID));

        RoleEntity targetRoleEntity = new RoleEntity(aRole(9, role));
        RoleEntity currentRoleEntity = new RoleEntity(aRole(ADMIN.getId(), "admin"));

        ServiceEntity serviceEntity = new ServiceEntity(singletonList("1"));
        serviceEntity.setExternalId(serviceExternalId);

        userEntity.addServiceRole(new ServiceRoleEntity(serviceEntity, currentRoleEntity));

        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(userEntity));
        when(roleDao.findByRoleName(role)).thenReturn(Optional.of(targetRoleEntity));
        when(serviceDao.countOfUsersWithRoleForService(serviceExternalId, ADMIN.getId())).thenReturn(2L);

        Optional<User> userOptional = serviceRoleUpdater.doUpdate(EXISTING_USER_EXTERNAL_ID, serviceExternalId, role);
        assertTrue(userOptional.isPresent());
        assertThat(userOptional.get().getRole().getId(), is(9));
    }

    private Role aRole(int roleId, String roleName) {
        return Role.role(roleId, roleName, roleName + "-description");
    }

    private User aUser(String externalId) {
        return User.from(randomInt(), externalId, "random-name", "random-password", "random@example.com",
                singletonList("1"), emptyList(), "784rh", "8948924", emptyList(), null,
                SecondFactorMethod.SMS, null, null);
    }
}
