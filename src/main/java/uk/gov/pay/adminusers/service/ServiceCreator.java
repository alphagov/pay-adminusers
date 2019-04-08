package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.pay.commons.model.SupportedLanguage;

import java.util.List;
import java.util.Map;

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
    public Service doCreate(List<String> gatewayAccountIds, Map<SupportedLanguage, String> serviceName) {
        ServiceEntity serviceEntity = ServiceEntity.from(Service.from());
        serviceName.forEach((language, name) -> serviceEntity.addOrUpdateServiceName(ServiceNameEntity.from(language, name)));

        if (!gatewayAccountIds.isEmpty()) {
            if (serviceDao.checkIfGatewayAccountsUsed(gatewayAccountIds)) {
                throw conflictingServiceGatewayAccounts(gatewayAccountIds);
            }
            serviceEntity.addGatewayAccountIds(gatewayAccountIds.toArray(new String[0]));
        }
        serviceDao.persist(serviceEntity);
        return linksBuilder.decorate(serviceEntity.toService());
    }
}
