package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.app.util.RandomIdGenerator;
import uk.gov.pay.adminusers.model.GoLiveStage;
import uk.gov.pay.adminusers.model.PspTestAccountStage;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.entity.GatewayAccountIdEntity;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntity;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntityBuilder;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.RandomUtils.nextInt;

public final class ServiceEntityFixture {
    private Integer id = nextInt();
    private String externalId = RandomIdGenerator.randomUuid();
    private String name = Service.DEFAULT_NAME_VALUE;
    private MerchantDetailsEntity merchantDetailsEntity = MerchantDetailsEntityBuilder.aMerchantDetailsEntity().build();
    private Map<String, Object> customBranding = Map.of("image_url", "image url", "css_url", "css url");
    private List<GatewayAccountIdEntity> gatewayAccountIds = new ArrayList<>();
    private Set<ServiceNameEntity> serviceName = new HashSet<>();
    private boolean redirectToServiceImmediatelyOnTerminalState = false;
    private boolean collectBillingAddress = true;
    private GoLiveStage goLiveStage = GoLiveStage.NOT_STARTED;
    private boolean experimentalFeaturesEnabled = false;
    private boolean takesPaymentsOverPhone = false;
    private ZonedDateTime createdDate = ZonedDateTime.parse("2020-06-29T01:16:00Z");
    private ZonedDateTime wentLiveDate;
    private String sector;
    private PspTestAccountStage pspTestAccountStage = PspTestAccountStage.NOT_STARTED;
    private boolean archived = false;

    private ServiceEntityFixture() {
    }

    public static ServiceEntityFixture aServiceEntity() {
        return new ServiceEntityFixture();
    }

    public ServiceEntityFixture withId(Integer id) {
        this.id = id;
        return this;
    }

    public ServiceEntityFixture withExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public ServiceEntityFixture withMerchantDetailsEntity(MerchantDetailsEntity merchantDetailsEntity) {
        this.merchantDetailsEntity = merchantDetailsEntity;
        return this;
    }

    public ServiceEntityFixture withCustomBranding(Map<String, Object> customBranding) {
        this.customBranding = customBranding;
        return this;
    }

    public ServiceEntityFixture withGatewayAccounts(List<GatewayAccountIdEntity> gatewayAccountIds) {
        this.gatewayAccountIds = gatewayAccountIds;
        return this;
    }

    public ServiceEntityFixture withServiceName(Set<ServiceNameEntity> serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public ServiceEntityFixture withServiceNameEntity(SupportedLanguage language, String name) {
        ServiceNameEntity entity = new ServiceNameEntity();
        entity.setLanguage(language);
        entity.setName(name);
        this.serviceName.add(entity);
        return this;
    }

    public ServiceEntityFixture withRedirectToServiceImmediatelyOnTerminalState(boolean redirectToServiceImmediatelyOnTerminalState) {
        this.redirectToServiceImmediatelyOnTerminalState = redirectToServiceImmediatelyOnTerminalState;
        return this;
    }

    public ServiceEntityFixture withCollectBillingAddress(boolean collectBillingAddress) {
        this.collectBillingAddress = collectBillingAddress;
        return this;
    }
    
    public ServiceEntityFixture withGoLiveStage(GoLiveStage goLiveStage) {
        this.goLiveStage = goLiveStage;
        return this;
    }

    public ServiceEntityFixture withExperimentalFeaturesEnabled(boolean experimentalFeaturesEnabled) {
        this.experimentalFeaturesEnabled = experimentalFeaturesEnabled;
        return this;
    }
    
    public ServiceEntityFixture withTakesPaymentsOverPhone(boolean takesPaymentsOverPhone) {
        this.takesPaymentsOverPhone = takesPaymentsOverPhone;
        return this;
    }
    public ServiceEntityFixture withCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public ServiceEntityFixture withWentLiveDate(ZonedDateTime wentLiveDate) {
        this.wentLiveDate = wentLiveDate;
        return this;
    }

    public ServiceEntityFixture withSector(String sector) {
        this.sector = sector;
        return this;
    }

    public ServiceEntityFixture withPspTestAccountStage(PspTestAccountStage pspTestAccountStage) {
        this.pspTestAccountStage = pspTestAccountStage;
        return this;
    }

    public ServiceEntityFixture withArchived(boolean archived) {
        this.archived = archived;
        return this;
    }

    public ServiceEntity build() {
        ServiceEntity serviceEntity = new ServiceEntity();
        serviceEntity.setId(id);
        serviceEntity.setExternalId(externalId);
        serviceEntity.setMerchantDetailsEntity(merchantDetailsEntity);
        serviceEntity.setCustomBranding(customBranding);
        serviceEntity.addOrUpdateServiceName(ServiceNameEntity.from(SupportedLanguage.ENGLISH, name));
        serviceName.forEach(serviceEntity::addOrUpdateServiceName);
        gatewayAccountIds.forEach(g -> serviceEntity.addGatewayAccountIds(g.getGatewayAccountId()));
        serviceEntity.setRedirectToServiceImmediatelyOnTerminalState(redirectToServiceImmediatelyOnTerminalState);
        serviceEntity.setCollectBillingAddress(collectBillingAddress);
        serviceEntity.setCurrentGoLiveStage(goLiveStage);
        serviceEntity.setExperimentalFeaturesEnabled(experimentalFeaturesEnabled);
        serviceEntity.setTakesPaymentsOverPhone(takesPaymentsOverPhone);
        serviceEntity.setCreatedDate(createdDate);
        serviceEntity.setWentLiveDate(wentLiveDate);
        serviceEntity.setSector(sector);
        serviceEntity.setCurrentPspTestAccountStage(pspTestAccountStage);
        serviceEntity.setArchived(archived);
        return serviceEntity;
    }
}
