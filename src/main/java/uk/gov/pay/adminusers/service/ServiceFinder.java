package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceSearchRequest;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

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
    
    public Optional<Service> byExternalId(String externalId) {
        return serviceDao.findByExternalId(externalId)
                .map(serviceEntity -> linksBuilder.decorate(serviceEntity.toService()));
    }
    
    public Map<String, List<?>> bySearchRequest(ServiceSearchRequest request) {
        var servicesByName = !isBlank(request.getServiceNameSearchString()) ? streamServiceEntitiesToServices(serviceDao
                .findByENServiceName(request.getServiceNameSearchString())) : Collections.emptyList();
        var servicesByMerchantName = !isBlank(request.getServiceMerchantNameSearchString()) ? streamServiceEntitiesToServices(serviceDao
                .findByServiceMerchantName(request.getServiceMerchantNameSearchString())) : Collections.emptyList();
        return Map.of("name_results", servicesByName, "merchant_results", servicesByMerchantName);
    }
    
    private List<Service> streamServiceEntitiesToServices (List<ServiceEntity> serviceEntities) {
        return serviceEntities.stream().map(serviceEntity -> linksBuilder.decorate(serviceEntity.toService()))
                .collect(Collectors.toUnmodifiableList());
    }
}
