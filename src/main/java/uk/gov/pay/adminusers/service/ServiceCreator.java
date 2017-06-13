package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import java.util.List;
import java.util.Optional;

import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingServiceGatewayAccounts;

public class ServiceCreator {

    private final ServiceDao serviceDao;
    private final LinksBuilder linksBuilder;

    @Inject
    public ServiceCreator(ServiceDao serviceDao, LinksBuilder linksBuilder) {
        this.serviceDao = serviceDao;
        this.linksBuilder = linksBuilder;
    }

    @Transactional
    public Service doCreate(Optional<String> serviceName, Optional<List<String>> gatewayAccountIdsOptional) {
        Service service = serviceName
                .map(name -> Service.from(name))
                .orElseGet(() -> Service.from());

        ServiceEntity serviceEntity = ServiceEntity.from(service);
        if (gatewayAccountIdsOptional.isPresent()) {
            List<String> gatewayAccountsIds = gatewayAccountIdsOptional.get();
            if (serviceDao.checkIfGatewayAccountsUsed(gatewayAccountsIds)) {
                throw conflictingServiceGatewayAccounts(gatewayAccountsIds);
            }
            serviceEntity.addGatewayAccountIds(gatewayAccountsIds.toArray(new String[0]));
        }
        serviceDao.persist(serviceEntity);
        return linksBuilder.decorate(serviceEntity.toService());
    }
}
