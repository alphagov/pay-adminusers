package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceCustomisations;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceCustomisationEntity;

import java.util.Optional;

public class ServiceCustomisationsUpdater {

    private final ServiceDao serviceDao;
    private final LinksBuilder linksBuilder;

    @Inject
    public ServiceCustomisationsUpdater(ServiceDao serviceDao, LinksBuilder linksBuilder) {
        this.serviceDao = serviceDao;
        this.linksBuilder = linksBuilder;
    }

    @Transactional
    public Optional<Service> doUpdate(String serviceExternalId, ServiceCustomisations serviceCustomisations) {
        return serviceDao.findByExternalId(serviceExternalId)
                .map(serviceEntity -> {
                    serviceEntity.setServiceCustomisationEntity(new ServiceCustomisationEntity(serviceCustomisations));
                    serviceDao.merge(serviceEntity);
                    return Optional.of(linksBuilder.decorate(serviceEntity.toService()));
                })
                .orElseGet(Optional::empty);
    }
}
