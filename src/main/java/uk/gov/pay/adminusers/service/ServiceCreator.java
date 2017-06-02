package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import java.util.List;
import java.util.Optional;

public class ServiceCreator {

    private final ServiceDao serviceDao;

    @Inject
    public ServiceCreator(ServiceDao serviceDao) {
        this.serviceDao = serviceDao;
    }

    @Transactional
    public Optional<Service> doCreate(Optional<String> serviceName, Optional<List<String>> gatewayAccountIds) {
        Service service = serviceName
                .map(name -> Service.from(name))
                .orElseGet(() -> Service.from());

        ServiceEntity serviceEntity = ServiceEntity.from(service);
        if(gatewayAccountIds.isPresent()) {
            serviceEntity.addGatewayAccountIds(gatewayAccountIds.get().toArray(new String[0]));
        }
        serviceDao.persist(serviceEntity);
        return Optional.of(serviceEntity.toService());
    }
}
