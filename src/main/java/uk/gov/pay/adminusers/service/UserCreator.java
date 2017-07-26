package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.CreateUserRequest;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.List;
import java.util.Optional;

import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingServiceGatewayAccountsForUser;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.undefinedRoleException;

public class UserCreator {

    private static final Logger logger = PayLoggerFactory.getLogger(UserCreator.class);

    private final UserDao userDao;
    private final RoleDao roleDao;
    private final ServiceDao serviceDao;
    private final PasswordHasher passwordHasher;
    private final LinksBuilder linksBuilder;

    @Inject
    public UserCreator(UserDao userDao, RoleDao roleDao, ServiceDao serviceDao, PasswordHasher passwordHasher, LinksBuilder linksBuilder) {
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.serviceDao = serviceDao;
        this.passwordHasher = passwordHasher;
        this.linksBuilder = linksBuilder;
    }

    @Transactional
    public User doCreate(CreateUserRequest userRequest, String roleName) {
        return roleDao.findByRoleName(roleName)
                .map(roleEntity -> {
                    UserEntity userEntity = UserEntity.from(userRequest);
                    userEntity.setPassword(passwordHasher.hash(userRequest.getPassword()));
                    if (!userRequest.getServiceExternalIds().isEmpty()) {
                        addServiceRoleToUser(userEntity, roleEntity, userRequest.getServiceExternalIds());
                    }
                    //Deprecated, leaving for backward compatibility
                    else if (userRequest.getGatewayAccountIds() != null && userRequest.getGatewayAccountIds().size() > 0) {
                        addServiceFromGatewayAccountsToUser(userEntity, roleEntity, userRequest.getGatewayAccountIds());
                    }
                    userDao.persist(userEntity);
                    return linksBuilder.decorate(userEntity.toUser());
                })
                .orElseThrow(() -> undefinedRoleException(roleName));
    }

    private void addServiceRoleToUser(UserEntity user, RoleEntity role, List<String> serviceExternalIds) {
        serviceExternalIds.forEach(serviceExternalId -> serviceDao.findByExternalId(serviceExternalId)
                .map(serviceEntity -> {
                    ServiceRoleEntity serviceRole = new ServiceRoleEntity(serviceEntity, role);
                    serviceRole.setUser(user);
                    user.addServiceRole(serviceRole);
                    return null;
                })
                .orElseGet(() -> {
                    logger.error("Unable to assign service with external id {} to user, as it does not exist", serviceExternalId);
                    return null;
                }));
    }

    private void addServiceFromGatewayAccountsToUser(UserEntity user, RoleEntity role, List<String> gatewayAccountIds) {
        ServiceRoleEntity serviceRole = getServiceAssignedTo(gatewayAccountIds)
                .map(serviceEntity -> new ServiceRoleEntity(serviceEntity, role))
                .orElseGet(() -> {
                    ServiceEntity service = new ServiceEntity(gatewayAccountIds);
                    serviceDao.persist(service);
                    return new ServiceRoleEntity(service, role);
                });
        serviceRole.setUser(user);
        user.addServiceRole(serviceRole);
    }

    private Optional<ServiceEntity> getServiceAssignedTo(List<String> gatewayAccountIds) {
        for (String gatewayAccountId : gatewayAccountIds) {
            Optional<ServiceEntity> serviceOptional = serviceDao.findByGatewayAccountId(gatewayAccountId);
            if (serviceOptional.isPresent()) {
                if (serviceOptional.get().hasExactGatewayAccountIds(gatewayAccountIds)) {
                    return serviceOptional;
                } else {
                    throw conflictingServiceGatewayAccountsForUser();
                }
            }
        }
        return Optional.empty();
    }
}
