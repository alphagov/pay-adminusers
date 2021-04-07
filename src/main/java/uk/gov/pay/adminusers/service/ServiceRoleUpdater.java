package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.apache.commons.lang3.StringUtils;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.Role;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.Optional;

import static uk.gov.pay.adminusers.service.AdminUsersExceptions.adminRoleLimitException;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingServiceForUser;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.serviceDoesNotExistError;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.undefinedRoleException;

public class ServiceRoleUpdater {

    private final UserDao userDao;
    private final ServiceDao serviceDao;
    private final RoleDao roleDao;
    private final LinksBuilder linksBuilder;

    private final Integer adminsPerServiceLimit = 1;

    @Inject
    public ServiceRoleUpdater(UserDao userDao, ServiceDao serviceDao, RoleDao roleDao, LinksBuilder linksBuilder) {
        this.userDao = userDao;
        this.serviceDao = serviceDao;
        this.roleDao = roleDao;
        this.linksBuilder = linksBuilder;
    }

    @Transactional
    public Optional<User> doUpdate(String userExternalId, String serviceId, String roleName) {
        String serviceExternalId = serviceId;
        if (StringUtils.isNumeric(serviceId)) {
            serviceExternalId = serviceDao.findById(Integer.valueOf(serviceId))
                    .map(ServiceEntity::getExternalId)
                    .orElseThrow(() -> serviceDoesNotExistError(serviceId));
        }

        Optional<UserEntity> userMaybe = userDao.findByExternalId(userExternalId);
        if (!userMaybe.isPresent()) {
            return Optional.empty();
        }
        UserEntity userEntity = userMaybe.get();

        Optional<RoleEntity> roleMaybe = roleDao.findByRoleName(roleName);
        if (!roleMaybe.isPresent()) {
            throw undefinedRoleException(roleName);
        }
        RoleEntity targetRoleEntity = roleMaybe.get();

        Optional<ServiceRoleEntity> servicesRoleMaybe = userEntity.getServicesRole(serviceExternalId);
        if (!servicesRoleMaybe.isPresent()) {
            throw conflictingServiceForUser(userEntity.getExternalId(), serviceExternalId);
        }

        ServiceRoleEntity serviceRoleEntity = servicesRoleMaybe.get();

        RoleEntity currentRoleEntity = serviceRoleEntity.getRole();

        if (currentRoleEntity.isAdmin() && !targetRoleEntity.isAdmin()) {
            if (serviceDao.countOfUsersWithRoleForService(serviceExternalId, Role.ADMIN.getId()) <= adminsPerServiceLimit) {
                throw adminRoleLimitException(adminsPerServiceLimit);
            }
        }
        serviceRoleEntity.setRole(targetRoleEntity);
        userEntity.addServiceRole(serviceRoleEntity);
        userDao.persist(userEntity);
        return Optional.of(linksBuilder.decorate(userEntity.toUser()));
    }
}
