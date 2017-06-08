package uk.gov.pay.adminusers.service;


import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceUpdateRequest;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static uk.gov.pay.adminusers.resources.ServiceRequestValidator.FIELD_GATEWAY_ACCOUNT_IDS;
import static uk.gov.pay.adminusers.resources.ServiceRequestValidator.FIELD_SERVICE_NAME;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingServiceGatewayAccounts;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.internalServerError;

public class ServiceUpdater {

    private final ServiceDao serviceDao;

    @Inject
    public ServiceUpdater(ServiceDao serviceDao) {
        this.serviceDao = serviceDao;
    }

    @Transactional
    public Optional<Service> doUpdate(String serviceExternalId, ServiceUpdateRequest serviceUpdateRequest) {
        return serviceDao.findByExternalId(serviceExternalId)
                .map(serviceEntity -> {
                    List<String> value = serviceUpdateRequest.getValue();
                    updateAttribute(serviceUpdateRequest, serviceEntity, value);
                    serviceDao.merge(serviceEntity);
                    return Optional.of(serviceEntity.toService());
                }).orElseGet(() -> Optional.empty());
    }

    private void updateAttribute(ServiceUpdateRequest serviceUpdateRequest, ServiceEntity serviceEntity, List<String> value) {
        String path = serviceUpdateRequest.getPath();
        if (path.equals(FIELD_SERVICE_NAME)) {
            updateServiceName(serviceEntity, value.get(0));
        } else {
            if(path.equals(FIELD_GATEWAY_ACCOUNT_IDS)) {
                assignGatewayAccounts(serviceEntity, value);
            } else {
                throw internalServerError(format("Invalid path value for updating service attribute: [%s]", path));
            }
        }
    }

    private void assignGatewayAccounts(ServiceEntity serviceEntity, List<String> gatewayAccountIds) {
        if (serviceDao.checkIfGatewayAccountsUsed(gatewayAccountIds)) {
            throw conflictingServiceGatewayAccounts(gatewayAccountIds);
        } else {
            serviceEntity.addGatewayAccountIds(gatewayAccountIds.toArray(new String[0]));
        }
    }

    private void updateServiceName(ServiceEntity serviceEntity, String nameToUpdate) {
        serviceEntity.setName(nameToUpdate);
    }
}
