package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;

import java.util.Optional;

public class ServiceFinder {

    private final ServiceDao serviceDao;
    private final LinksBuilder linksBuilder;

    @Inject
    public ServiceFinder(ServiceDao serviceDao, LinksBuilder linksBuilder) {
        this.serviceDao = serviceDao;
        this.linksBuilder = linksBuilder;
    }

    public Optional<Service> byGatewayAccountId(String gatewayAccountId) {
        return serviceDao.findByGatewayAccountId(gatewayAccountId)
                .map(serviceEntity -> linksBuilder.decorate(serviceEntity.toService()));
    }
}
