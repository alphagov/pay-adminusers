package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.Optional;

import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingServiceRoleForUser;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.serviceDoesNotExistError;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.undefinedRoleException;

public class ServiceRoleCreator {

    private final UserDao userDao;
    private final ServiceDao serviceDao;
    private final RoleDao roleDao;
    private final LinksBuilder linksBuilder;

    @Inject
    public ServiceRoleCreator(UserDao userDao, ServiceDao serviceDao, RoleDao roleDao, LinksBuilder linksBuilder) {
        this.userDao = userDao;
        this.serviceDao = serviceDao;
        this.roleDao = roleDao;
        this.linksBuilder = linksBuilder;
    }

    @Transactional
    public Optional<User> doCreate(String userExternalId, String serviceExternalId, RoleName roleName) {
        Optional<UserEntity> userMaybe = userDao.findByExternalId(userExternalId);
        if (userMaybe.isEmpty()) {
            return Optional.empty();
        }

        Optional<ServiceEntity> serviceMaybe = serviceDao.findByExternalId(serviceExternalId);
        if (serviceMaybe.isEmpty()) {
            throw serviceDoesNotExistError(serviceExternalId);
        }

        Optional<RoleEntity> roleMaybe = roleDao.findByRoleName(roleName);
        if (roleMaybe.isEmpty()) {
            throw undefinedRoleException(roleName.toString());
        }

        UserEntity userEntity = userMaybe.get();
        userEntity.getServicesRole(serviceExternalId)
                .ifPresent(serviceRoleEntity -> {
                    throw conflictingServiceRoleForUser(userExternalId, serviceExternalId);
                });

        userEntity.addServiceRole(new ServiceRoleEntity(serviceMaybe.get(), roleMaybe.get()));
        userDao.merge(userEntity);

        return Optional.of(linksBuilder.decorate(userEntity.toUser()));
    }
}
