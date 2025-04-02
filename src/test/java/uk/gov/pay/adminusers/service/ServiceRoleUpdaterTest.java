package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

@ExtendWith(MockitoExtension.class)
public class ServiceRoleUpdaterTest {

    @Mock
    private UserDao userDao;
    @Mock
    private RoleDao roleDao;
    @Mock
    private ServiceDao serviceDao;

    private ServiceRoleUpdater serviceRoleUpdater;

    private static final String EXISTING_USER_EXTERNAL_ID = "7d19aff33f8948deb97ed16b2912dcd3";
    private static final String NON_EXISTENT_USER_EXTERNAL_ID = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    private static final Role adminRole = new Role(2, RoleName.ADMIN, "Administrator");
    private static final Role viewRole = new Role(4, RoleName.VIEW_ONLY, "View only");

    @BeforeEach
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
        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID))));

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> serviceRoleUpdater.doUpdate(EXISTING_USER_EXTERNAL_ID, randomUuid(), "randomRole"));
        assertThat(exception.getMessage(), is("HTTP 400 Bad Request"));
    }

    @Test
    public void shouldError_ifServiceNotBelongToUser_whenUpdatingServiceRole() {
        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID))));
        when(roleDao.findByRoleName(RoleName.ADMIN)).thenReturn(Optional.of(new RoleEntity(adminRole)));

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> serviceRoleUpdater.doUpdate(EXISTING_USER_EXTERNAL_ID, randomUuid(), RoleName.ADMIN.getName()));
        assertThat(exception.getMessage(), is("HTTP 409 Conflict"));
    }

    @Test
    public void shouldError_ifCountOfServiceAdminsLessThan1_whenUpdatingServiceRole() {
        String serviceExternalId = "sxrdctfvygbuhinj";

        UserEntity userEntity = UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID));

        RoleEntity currentRoleEntity = new RoleEntity(adminRole);

        ServiceEntity serviceEntity = new ServiceEntity(Collections.singletonList("1"));
        serviceEntity.setExternalId(serviceExternalId);

        userEntity.addServiceRole(new ServiceRoleEntity(serviceEntity, currentRoleEntity));

        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(userEntity));
        when(roleDao.findByRoleName(viewRole.getRoleName())).thenReturn(Optional.of(new RoleEntity(viewRole)));
        when(serviceDao.countOfUsersWithRoleForService(serviceExternalId, ADMIN.getId())).thenReturn(1L);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> serviceRoleUpdater.doUpdate(EXISTING_USER_EXTERNAL_ID, serviceExternalId, viewRole.getRoleName().getName()));
        assertThat(exception.getMessage(), is("HTTP 412 Precondition Failed"));
    }

    @Test
    public void shouldReturnUpdatedUser_whenUpdatingServiceRoleSuccess() {
        String serviceExternalId = "sxrdctfvygbuhinj";

        UserEntity userEntity = UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID));

        ServiceEntity serviceEntity = new ServiceEntity(Collections.singletonList("1"));
        serviceEntity.addOrUpdateServiceName(ServiceNameEntity.from(SupportedLanguage.ENGLISH, Service.DEFAULT_NAME_VALUE));
        serviceEntity.setExternalId(serviceExternalId);

        userEntity.addServiceRole(new ServiceRoleEntity(serviceEntity, new RoleEntity(viewRole)));

        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(userEntity));
        when(roleDao.findByRoleName(adminRole.getRoleName())).thenReturn(Optional.of(new RoleEntity(adminRole)));

        Optional<User> userOptional = serviceRoleUpdater.doUpdate(EXISTING_USER_EXTERNAL_ID, serviceExternalId, adminRole.getRoleName().getName());
        assertTrue(userOptional.isPresent());
        // TODO: this looks like a bug in the updater, should only be 1 service role
        assertThat(userOptional.get().getServiceRoles(), hasSize(2));
        assertThat(userOptional.get().getServiceRoles().get(0).getRole().getId(), is(adminRole.getId()));
    }

    @Test
    public void shouldReturnUpdatedUser_whenDowngradingAdminWhenEnoughAdminsSuccess() {
        String serviceExternalId = "sxrdctfvygbuhinj";

        UserEntity userEntity = UserEntity.from(aUser(EXISTING_USER_EXTERNAL_ID));

        ServiceEntity serviceEntity = new ServiceEntity(Collections.singletonList("1"));
        serviceEntity.addOrUpdateServiceName(ServiceNameEntity.from(SupportedLanguage.ENGLISH, Service.DEFAULT_NAME_VALUE));
        serviceEntity.setExternalId(serviceExternalId);

        userEntity.addServiceRole(new ServiceRoleEntity(serviceEntity, new RoleEntity(adminRole)));

        when(userDao.findByExternalId(EXISTING_USER_EXTERNAL_ID)).thenReturn(Optional.of(userEntity));
        when(roleDao.findByRoleName(viewRole.getRoleName())).thenReturn(Optional.of(new RoleEntity(viewRole)));
        when(serviceDao.countOfUsersWithRoleForService(serviceExternalId, ADMIN.getId())).thenReturn(2L);

        Optional<User> userOptional = serviceRoleUpdater.doUpdate(EXISTING_USER_EXTERNAL_ID, serviceExternalId, viewRole.getRoleName().getName());
        assertTrue(userOptional.isPresent());
        // TODO: this looks like a bug in the updater, should only be 1 service role
        assertThat(userOptional.get().getServiceRoles(), hasSize(2));
        assertThat(userOptional.get().getServiceRoles().get(0).getRole().getId(), is(viewRole.getId()));
    }

    private User aUser(String externalId) {
        return User.from(randomInt(), externalId, "random-password", "random@example.com",
                "784rh", "8948924", emptyList(), null, SecondFactorMethod.SMS,
                null, null, null);
    }
}
