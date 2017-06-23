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

        ServiceRoleEntity userServiceRoleToRemove = getServiceRoleEntityOf(userExternalId, serviceExternalId);

        checkRemoverIsAdmin(removerExternalId, serviceExternalId)
                .orElseThrow(() -> forbiddenOperationException(userExternalId, OPERATION, serviceExternalId));

        serviceRoleDao.remove(userServiceRoleToRemove);
    }

    private Optional<ServiceRoleEntity> checkRemoverIsAdmin(String removerExternalId, String serviceExternalId) {
        return userDao.findByExternalId(removerExternalId)
                .flatMap(removerEntity -> removerEntity.getServicesRole(serviceExternalId))
                .filter(isRoleAdmin());
    }

    private ServiceRoleEntity getServiceRoleEntityOf(String userExternalId, String serviceExternalId) {
        return userDao.findByExternalId(userExternalId)
                .map(userEntity -> userEntity.getServicesRole(serviceExternalId)
                        .orElseThrow(AdminUsersExceptions::notFoundException))
                .orElseThrow(AdminUsersExceptions::notFoundException);
    }

    private Predicate<ServiceRoleEntity> isRoleAdmin() {
        return serviceRoleEntity -> serviceRoleEntity.getRole().isAdmin();
    }
}
