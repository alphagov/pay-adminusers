package uk.gov.pay.adminusers.service;


import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceUpdateRequest;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;

import java.util.Optional;

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
                    serviceEntity.setName(serviceUpdateRequest.getValue().get(0));
                    serviceDao.merge(serviceEntity);
                    return Optional.of(serviceEntity.toService());
                }).orElseGet(() -> Optional.empty());
    }
}
