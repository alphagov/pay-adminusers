package uk.gov.pay.adminusers.service;

import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.persistence.dao.ServiceRoleDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;

import javax.inject.Inject;
import java.util.Optional;
import java.util.function.Predicate;

import static uk.gov.pay.adminusers.service.AdminUsersExceptions.forbiddenOperationException;

public class ServiceUserRemover {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(ServiceUserRemover.class);
    private static final String OPERATION = "remove user";

    private final UserDao userDao;
    private final ServiceRoleDao serviceRoleDao;

    @Inject
    public ServiceUserRemover(UserDao userDao, ServiceRoleDao serviceRoleDao) {
        this.userDao = userDao;
        this.serviceRoleDao = serviceRoleDao;
    }

    public void remove(String userExternalId, String removerExternalId, String serviceExternalId) {
        LOGGER.info("User remove from service requested - serviceId={}, removerId={}, userId={}", serviceExternalId, removerExternalId, userExternalId);
        userDao.findByExternalId(userExternalId)
                .flatMap(userEntity -> userEntity.getServicesRole(serviceExternalId)
                        .map(serviceUserRole -> userDao.findByExternalId(removerExternalId)
                                .map(removerEntity -> removerEntity.getServicesRole(serviceExternalId)
                                        .filter(isRoleAdmin())
                                        .map(serviceRemoverRole -> Optional.of(serviceUserRole))
                                        .orElseThrow(() -> forbiddenOperationException(userExternalId, OPERATION, serviceExternalId)))
                                .orElseThrow(() -> forbiddenOperationException(userExternalId, OPERATION, serviceExternalId))))
                .orElseThrow(AdminUsersExceptions::notFoundException)
                .ifPresent(serviceRoleDao::remove);
    }

    private Predicate<ServiceRoleEntity> isRoleAdmin() {
        return serviceRoleEntity -> serviceRoleEntity.getRole().isAdmin();
    }
}
