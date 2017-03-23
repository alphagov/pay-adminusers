package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.Optional;

import static uk.gov.pay.adminusers.model.Role.ROLE_ADMIN_ID;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.*;

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

    /**
     * updates user's service role.
     *
     * @param usernameOrExternalId
     * @param serviceId
     * @param roleName
     * @return Updated User if successful or Optional.empty() if user not found
     */
    @Transactional
    public Optional<User> doUpdate(String usernameOrExternalId, Integer serviceId, String roleName){
        Optional<UserEntity> userMaybe = userDao.findByUsername(usernameOrExternalId);
        if (!userMaybe.isPresent()) {
            userMaybe = userDao.findByExternalId(usernameOrExternalId);
        }
        if(!userMaybe.isPresent()){
            return Optional.empty();
        }
        UserEntity userEntity = userMaybe.get();

        Optional<RoleEntity> roleMaybe = roleDao.findByRoleName(roleName);
        if(!roleMaybe.isPresent()){
            throw undefinedRoleException(roleName);
        }
        RoleEntity roleEntity = roleMaybe.get();

        Optional<ServiceRoleEntity> servicesRoleMaybe = userEntity.getServicesRole(serviceId);
        if(!servicesRoleMaybe.isPresent()) {
            throw conflictingServiceForUser(userEntity.getId(), serviceId);
        }

        ServiceRoleEntity serviceRoleEntity = servicesRoleMaybe.get();

        if (!roleEntity.getId().equals(ROLE_ADMIN_ID)) {
            if (serviceDao.countOfRolesForService(serviceId, ROLE_ADMIN_ID) <= adminsPerServiceLimit) {
                throw adminRoleLimitException(adminsPerServiceLimit);
            }
        }
        serviceRoleEntity.setRole(roleEntity);
        userEntity.setServiceRole(serviceRoleEntity);
        userDao.persist(userEntity);
        return Optional.of(linksBuilder.decorate(userEntity.toUser()));
    }
}
