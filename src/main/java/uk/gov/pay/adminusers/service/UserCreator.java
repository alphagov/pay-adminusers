package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.CreateUserRequest;
import uk.gov.pay.adminusers.model.RoleName;
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

import java.util.List;
import java.util.Optional;

import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingServiceGatewayAccountsForUser;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.undefinedRoleException;

public class UserCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserCreator.class);

    private final UserDao userDao;
    private final RoleDao roleDao;
    private final ServiceDao serviceDao;
    private final PasswordHasher passwordHasher;
    private final LinksBuilder linksBuilder;
    private final SecondFactorAuthenticator secondFactorAuthenticator;

    @Inject
    public UserCreator(UserDao userDao,
                       RoleDao roleDao,
                       ServiceDao serviceDao,
                       PasswordHasher passwordHasher,
                       LinksBuilder linksBuilder,
                       SecondFactorAuthenticator secondFactorAuthenticator) {
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.serviceDao = serviceDao;
        this.passwordHasher = passwordHasher;
        this.linksBuilder = linksBuilder;
        this.secondFactorAuthenticator = secondFactorAuthenticator;
    }

    @Transactional
    public User doCreate(CreateUserRequest userRequest, String roleName) {
        return roleDao.findByRoleName(RoleName.fromName(roleName))
                .map(roleEntity -> {
                    String otpKey = userRequest.getOtpKey().orElseGet(secondFactorAuthenticator::generateNewBase32EncodedSecret);
                    UserEntity userEntity = UserEntity.from(userRequest, otpKey);
                    userEntity.setPassword(passwordHasher.hash(userRequest.getPassword()));
                    if (hasServiceIds(userRequest)) {
                        addServiceRoleToUser(userEntity, roleEntity, userRequest.getServiceExternalIds());
                    }
                    // Deprecated, leaving for backward compatibility
                    else if (hasGatewayAccountIds(userRequest)) {
                        addServiceFromGatewayAccountsToUser(userEntity, roleEntity, userRequest.getGatewayAccountIds());
                    }
                    userDao.persist(userEntity);
                    return linksBuilder.decorate(userEntity.toUser());
                })
                .orElseThrow(() -> undefinedRoleException(roleName));
    }

    private static boolean hasServiceIds(CreateUserRequest userRequest) {
        return userRequest.getServiceExternalIds() != null && !userRequest.getServiceExternalIds().isEmpty();
    }

    private static boolean hasGatewayAccountIds(CreateUserRequest userRequest) {
        return userRequest.getGatewayAccountIds() != null && !userRequest.getGatewayAccountIds().isEmpty();
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
                    LOGGER.error("Unable to assign service with external id {} to user, as it does not exist", serviceExternalId);
                    return null;
                }));
    }

    private void addServiceFromGatewayAccountsToUser(UserEntity user, RoleEntity role, List<String> gatewayAccountIds) {
        ServiceRoleEntity serviceRole = getServiceAssignedTo(gatewayAccountIds)
                .map(serviceEntity -> new ServiceRoleEntity(serviceEntity, role))
                .orElseGet(() -> {
                    ServiceEntity service = new ServiceEntity(gatewayAccountIds);
                    service.addOrUpdateServiceName(ServiceNameEntity.from(SupportedLanguage.ENGLISH, Service.DEFAULT_NAME_VALUE));
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
