package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.persistence.dao.ServiceRoleDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.ws.rs.WebApplicationException;
import java.util.Optional;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

@RunWith(MockitoJUnitRunner.class)
public class ServiceUserRemoverTest {

    private ServiceUserRemover service;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Mock
    private UserDao mockUserDao;

    @Mock
    private ServiceRoleDao mockServiceRoleDao;

    @Before
    public void setupServiceUserRemover() {
        service = new ServiceUserRemover(mockUserDao, mockServiceRoleDao);
    }

    @Test
    public void remove_shouldRemoveAUserFromAService() {

        String serviceExternalId = "service-external-id-1";
        String removerExternalId = "user-admin-of-service-1";
        String userExternalId = "user-to-be-removed-from-service-1";

        ServiceRoleEntity userServiceRole = aServiceRole(serviceExternalId, 666);
        UserEntity userToBeRemoved = createUser(userExternalId, userServiceRole);
        UserEntity removerAsAdminOfService = createUser(removerExternalId, aServiceRole(serviceExternalId, ADMIN.getId()));

        when(mockUserDao.findByExternalId(userExternalId)).thenReturn(Optional.of(userToBeRemoved));
        when(mockUserDao.findByExternalId(removerExternalId)).thenReturn(Optional.of(removerAsAdminOfService));

        service.remove(userExternalId, removerExternalId, serviceExternalId);

        verify(mockServiceRoleDao).remove(userServiceRole);
    }

    @Test
    public void remove_shouldThrowNotFoundWebApplicationException_whenUserToBeRemovedDoesNotExist() {

        String serviceExternalId = "service-external-id-1";
        String userExternalId = "user-to-be-removed-from-a-service";

        when(mockUserDao.findByExternalId(userExternalId)).thenReturn(Optional.empty());

        expectedException.expect(WebApplicationException.class);
        expectedException.expectMessage(is("HTTP 404 Not Found"));

        service.remove(userExternalId, "any-remover-ext-id", serviceExternalId);

        verifyZeroInteractions(mockServiceRoleDao);
        verifyNoMoreInteractions(mockUserDao);
    }

    @Test
    public void remove_shouldThrowNotFoundWebApplicationException_whenUserDoesNotBelongToTheGivenService() {

        String serviceExternalId = "service-external-id-1";
        String otherServiceExternalId = "service-external-id-2";
        String aRemoverExternalId = "user-admin-of-service-1";
        String userExternalId = "user-to-be-removed-from-service-1";

        UserEntity userToRemoveBelongsToOtherService = createUser(userExternalId, aServiceRole(otherServiceExternalId, 666));

        when(mockUserDao.findByExternalId(userExternalId)).thenReturn(Optional.of(userToRemoveBelongsToOtherService));

        expectedException.expect(WebApplicationException.class);
        expectedException.expectMessage(is("HTTP 404 Not Found"));

        service.remove(userExternalId, aRemoverExternalId, serviceExternalId);

        verifyZeroInteractions(mockServiceRoleDao);
        verifyNoMoreInteractions(mockUserDao);
    }

    @Test
    public void remove_shouldThrowForbiddenWebApplicationException_whenRemoverDoesNotExist() {

        String serviceExternalId = "service-external-id-1";
        String removerExternalId = "non-existing-remover";
        String userExternalId = "user-to-be-removed-from-service-1";

        UserEntity userToBeRemoved = createUser(userExternalId, aServiceRole(serviceExternalId, 666));

        when(mockUserDao.findByExternalId(userExternalId)).thenReturn(Optional.of(userToBeRemoved));
        when(mockUserDao.findByExternalId(removerExternalId)).thenReturn(Optional.empty());

        expectedException.expect(WebApplicationException.class);
        expectedException.expectMessage(is("HTTP 403 Forbidden"));

        service.remove(userExternalId, removerExternalId, serviceExternalId);

        verifyZeroInteractions(mockServiceRoleDao);
    }

    @Test
    public void remove_shouldThrowForbiddenWebApplicationException_whenRemoverDoesNotBelongToService() {

        String serviceExternalId = "service-external-id-1";
        String otherServiceExternalId = "service-external-id-2";
        String removerExternalId = "user-admin-of-service-1";
        String userExternalId = "user-to-be-removed-from-service-1";

        UserEntity userToBeRemoved = createUser(userExternalId, aServiceRole(serviceExternalId, 666));
        UserEntity removerAsAdminOfOtherService = createUser(removerExternalId, aServiceRole(otherServiceExternalId, ADMIN.getId()));

        when(mockUserDao.findByExternalId(userExternalId)).thenReturn(Optional.of(userToBeRemoved));
        when(mockUserDao.findByExternalId(removerExternalId)).thenReturn(Optional.of(removerAsAdminOfOtherService));

        expectedException.expect(WebApplicationException.class);
        expectedException.expectMessage(is("HTTP 403 Forbidden"));

        service.remove(userExternalId, removerExternalId, serviceExternalId);

        verifyZeroInteractions(mockServiceRoleDao);
    }

    @Test
    public void remove_shouldThrowForbiddenWebApplicationException_whenRemoverHasNotAdminRoleForTheGivenService() {

        String serviceExternalId = "service-external-id-1";
        String removerExternalId = "user-admin-of-service-1";
        String userExternalId = "user-to-be-removed-from-service-1";

        UserEntity userToBeRemoved = createUser(userExternalId, aServiceRole(serviceExternalId, 666));
        UserEntity removerAsNoAdminOfService = createUser(removerExternalId, aServiceRole(serviceExternalId, 999));

        when(mockUserDao.findByExternalId(userExternalId)).thenReturn(Optional.of(userToBeRemoved));
        when(mockUserDao.findByExternalId(removerExternalId)).thenReturn(Optional.of(removerAsNoAdminOfService));

        expectedException.expect(WebApplicationException.class);
        expectedException.expectMessage(is("HTTP 403 Forbidden"));

        service.remove(userExternalId, removerExternalId, serviceExternalId);

        verifyZeroInteractions(mockServiceRoleDao);
    }

    private UserEntity createUser(String externalId, ServiceRoleEntity serviceRole) {
        final UserEntity user = new UserEntity();
        user.setExternalId(externalId);
        user.setExternalId(random(10));
        user.addServiceRole(serviceRole);
        return user;
    }

    private ServiceRoleEntity aServiceRole(String serviceExternalId, int roleId) {
        ServiceEntity serviceEntity = new ServiceEntity();
        serviceEntity.setExternalId(serviceExternalId);
        RoleEntity role = new RoleEntity();
        role.setId(roleId);
        return new ServiceRoleEntity(serviceEntity, role);
    }
}
