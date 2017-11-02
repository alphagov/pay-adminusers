package uk.gov.pay.adminusers.service;


import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.exception.ServiceNotFoundException;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceUpdateRequest;
import uk.gov.pay.adminusers.model.UpdateMerchantDetailsRequest;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import static uk.gov.pay.adminusers.resources.ServiceRequestValidator.*;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingServiceGatewayAccounts;

public class ServiceUpdater {
    private final ServiceDao serviceDao;

    private final Map<String, BiConsumer<ServiceUpdateRequest, ServiceEntity>> attributeUpdaters = new HashMap<String, BiConsumer<ServiceUpdateRequest, ServiceEntity>>() {{
        put(FIELD_SERVICE_NAME, updateServiceName());
        put(FIELD_GATEWAY_ACCOUNT_IDS, assignGatewayAccounts());
        put(FIELD_CUSTOM_BRANDING, updateCustomBranding());
    }};

    @Inject
    public ServiceUpdater(ServiceDao serviceDao) {
        this.serviceDao = serviceDao;
    }

    @Transactional
    public Optional<Service> doUpdate(String serviceExternalId, ServiceUpdateRequest serviceUpdateRequest) {
        return serviceDao.findByExternalId(serviceExternalId)
                .flatMap(serviceEntity -> {
                    attributeUpdaters.get(serviceUpdateRequest.getPath())
                            .accept(serviceUpdateRequest, serviceEntity);
                    serviceDao.merge(serviceEntity);
                    return Optional.of(serviceEntity.toService());
                });
    }

    @Transactional
    public Service doUpdateMerchantDetails(String serviceExternalId, UpdateMerchantDetailsRequest updateMerchantDetailsRequest) throws ServiceNotFoundException {
        return serviceDao.findByExternalId(serviceExternalId)
                .map(serviceEntity -> {
                    MerchantDetailsEntity merchantEntity = MerchantDetailsEntity.from(updateMerchantDetailsRequest);
                    serviceEntity.setMerchantDetailsEntity(merchantEntity);
                    serviceDao.merge(serviceEntity);
                    return serviceEntity.toService();
                }).orElseThrow(() -> new ServiceNotFoundException(serviceExternalId));
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateServiceName() {
        return (serviceUpdateRequest, serviceEntity) -> serviceEntity.setName(serviceUpdateRequest.valueAsString());
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> assignGatewayAccounts() {
        return (serviceUpdateRequest, serviceEntity) -> {
            List<String> gatewayAccountIds = serviceUpdateRequest.valueAsList();
            if (serviceDao.checkIfGatewayAccountsUsed(gatewayAccountIds)) {
                throw conflictingServiceGatewayAccounts(gatewayAccountIds);
            } else {
                serviceEntity.addGatewayAccountIds(gatewayAccountIds.toArray(new String[0]));
            }
        };
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateCustomBranding() {
        return (serviceUpdateRequest, serviceEntity) -> serviceEntity.setCustomBranding(serviceUpdateRequest.valueAsObject());
    }
}
