package uk.gov.pay.adminusers.persistence.entity;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.math.RandomUtils;
import uk.gov.pay.adminusers.app.util.RandomIdGenerator;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.pay.commons.model.SupportedLanguage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ServiceEntityBuilder {
    private Integer id = RandomUtils.nextInt();
    private String externalId = RandomIdGenerator.randomUuid();
    private String name = Service.DEFAULT_NAME_VALUE;
    private MerchantDetailsEntity merchantDetailsEntity = MerchantDetailsEntityBuilder.aMerchantDetailsEntity().build();
    private Map<String, Object> customBranding = ImmutableMap.of("image_url", "image url", "css_url", "css url");
    private List<GatewayAccountIdEntity> gatewayAccountIds = new ArrayList<>();
    private Set<ServiceNameEntity> serviceName = new HashSet<>();
    private boolean redirectToServiceImmediatelyOnTerminalState = false;
    private boolean collectBillingAddress = true;

    private ServiceEntityBuilder() {
    }

    public static ServiceEntityBuilder aServiceEntity() {
        return new ServiceEntityBuilder();
    }

    public ServiceEntityBuilder withId(Integer id) {
        this.id = id;
        return this;
    }

    public ServiceEntityBuilder withExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public ServiceEntityBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ServiceEntityBuilder withMerchantDetailsEntity(MerchantDetailsEntity merchantDetailsEntity) {
        this.merchantDetailsEntity = merchantDetailsEntity;
        return this;
    }

    public ServiceEntityBuilder withCustomBranding(Map<String, Object> customBranding) {
        this.customBranding = customBranding;
        return this;
    }

    public ServiceEntityBuilder withGatewayAccounts(List<GatewayAccountIdEntity> gatewayAccountIds) {
        this.gatewayAccountIds = gatewayAccountIds;
        return this;
    }

    public ServiceEntityBuilder withServiceName(Set<ServiceNameEntity> serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public ServiceEntityBuilder withServiceNameEntity(SupportedLanguage language, String name) {
        ServiceNameEntity entity = new ServiceNameEntity();
        entity.setLanguage(language);
        entity.setName(name);
        this.serviceName.add(entity);
        return this;
    }

    public ServiceEntityBuilder withRedirectToServiceImmediatelyOnTerminalState(boolean redirectToServiceImmediatelyOnTerminalState) {
        this.redirectToServiceImmediatelyOnTerminalState = redirectToServiceImmediatelyOnTerminalState;
        return this;
    }

    public ServiceEntityBuilder withCollectBillingAddress(boolean collectBillingAddress) {
        this.collectBillingAddress = collectBillingAddress;
        return this;
    }

    public ServiceEntity build() {
        ServiceEntity serviceEntity = new ServiceEntity();
        serviceEntity.setId(id);
        serviceEntity.setExternalId(externalId);
        serviceEntity.setName(name);
        serviceEntity.setMerchantDetailsEntity(merchantDetailsEntity);
        serviceEntity.setCustomBranding(customBranding);
        serviceName.forEach(serviceEntity::addOrUpdateServiceName);
        gatewayAccountIds.forEach(g -> serviceEntity.addGatewayAccountIds(g.getGatewayAccountId()));
        serviceEntity.setRedirectToServiceImmediatelyOnTerminalState(redirectToServiceImmediatelyOnTerminalState);
        serviceEntity.setCollectBillingAddress(collectBillingAddress);
        return serviceEntity;
    }
}
