package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.pay.adminusers.persistence.entity.service.SupportedLanguage;

import java.util.List;
import java.util.Map;
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
    public Service doCreate(Optional<String> serviceName,
                            Optional<List<String>> gatewayAccountIdsOptional,
                            Map<SupportedLanguage, String> serviceNameVariants) {
        Service service = serviceName
                .map(Service::from)
                .orElseGet(Service::from);

        ServiceEntity serviceEntity = ServiceEntity.from(service);
        serviceEntity.addOrUpdateServiceName(ServiceNameEntity.from(SupportedLanguage.ENGLISH, service.getName()));
        serviceNameVariants.forEach((language, name) -> serviceEntity.addOrUpdateServiceName(ServiceNameEntity.from(language, name)));

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
