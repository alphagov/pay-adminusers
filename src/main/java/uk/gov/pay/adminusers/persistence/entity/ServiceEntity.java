package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.model.GoLiveStage;
import uk.gov.pay.adminusers.model.PspTestAccountStage;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceName;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.pay.commons.model.SupportedLanguage;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static javax.persistence.EnumType.STRING;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

@Entity
@Table(name = "services")
@SequenceGenerator(name = "services_seq_gen", sequenceName = "services_id_seq", allocationSize = 1)
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "services_seq_gen")
    private Integer id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "redirect_to_service_immediately_on_terminal_state")
    private boolean redirectToServiceImmediatelyOnTerminalState = false;

    @Column(name = "collect_billing_address")
    private boolean collectBillingAddress = true;

    @Embedded
    private MerchantDetailsEntity merchantDetailsEntity;

    @Column(name = "custom_branding", columnDefinition = "json")
    @Convert(converter = CustomBrandingConverter.class)
    private Map<String, Object> customBranding;

    @OneToMany(mappedBy = "service", targetEntity = GatewayAccountIdEntity.class, fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    private List<GatewayAccountIdEntity> gatewayAccountIds = new ArrayList<>();

    @OneToMany(mappedBy = "service", targetEntity = InviteEntity.class, fetch = FetchType.LAZY)
    private List<InviteEntity> invites = new ArrayList<>();

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<ServiceNameEntity> serviceNames = new HashSet<>();
    
    @Column(name = "current_go_live_stage")
    @Enumerated(STRING)
    private GoLiveStage currentGoLiveStage = GoLiveStage.NOT_STARTED;

    @Column(name = "experimental_features_enabled")
    private boolean experimentalFeaturesEnabled = false;

    @Column(name = "agent_initiated_moto_enabled")
    private boolean agentInitiatedMotoEnabled;
    
    @Column(name = "sector")
    private String sector;
    
    @Column(name = "internal")
    private boolean internal;
    
    @Column(name = "archived")
    private boolean archived;
    
    @Column(name = "created_date")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime createdDate = ZonedDateTime.now(UTC);
    
    @Column(name = "went_live_date")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime wentLiveDate;

    @Column(name = "current_psp_test_account_stage")
    @Enumerated(STRING)
    private PspTestAccountStage currentPspTestAccountStage = PspTestAccountStage.NOT_STARTED;

    public ServiceEntity() {
    }

    public ServiceEntity(List<String> gatewayAccountIds) {
        this.gatewayAccountIds.clear();
        this.externalId = randomUuid();
        this.redirectToServiceImmediatelyOnTerminalState = false;
        this.collectBillingAddress = true;
        populateGatewayAccountIds(gatewayAccountIds);
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean isRedirectToServiceImmediatelyOnTerminalState() {
        return redirectToServiceImmediatelyOnTerminalState;
    }

    public void setRedirectToServiceImmediatelyOnTerminalState(boolean redirectToServiceImmediatelyOnTerminalState) {
        this.redirectToServiceImmediatelyOnTerminalState = redirectToServiceImmediatelyOnTerminalState;
    }

    public boolean isCollectBillingAddress() {
        return collectBillingAddress;
    }

    public void setCollectBillingAddress(boolean collectBillingAddress) {
        this.collectBillingAddress = collectBillingAddress;
    }

    public MerchantDetailsEntity getMerchantDetailsEntity() {
        return merchantDetailsEntity;
    }

    public void setMerchantDetailsEntity(MerchantDetailsEntity merchantDetailsEntity) {
        this.merchantDetailsEntity = merchantDetailsEntity;
    }

    public List<GatewayAccountIdEntity> getGatewayAccountIds() {
        return List.copyOf(this.gatewayAccountIds);
    }

    public GatewayAccountIdEntity getGatewayAccountId() {
        return gatewayAccountIds.get(0);
    }

    public List<InviteEntity> getInvites() {
        return invites;
    }

    public void addGatewayAccountIds(String... gatewayAccountIds) {
        populateGatewayAccountIds(asList(gatewayAccountIds));
    }

    public Map<String, Object> getCustomBranding() {
        return customBranding;
    }

    public void setCustomBranding(Map<String, Object> customBranding) {
        this.customBranding = customBranding;
    }

    public GoLiveStage getCurrentGoLiveStage() {
        return currentGoLiveStage;
    }

    public void setCurrentGoLiveStage(GoLiveStage currentGoLiveStage) {
        this.currentGoLiveStage = currentGoLiveStage;
    }

    public boolean isExperimentalFeaturesEnabled() {
        return experimentalFeaturesEnabled;
    }

    public void setExperimentalFeaturesEnabled(boolean experimentalFeaturesEnabled) {
        this.experimentalFeaturesEnabled = experimentalFeaturesEnabled;
    }

    public boolean isAgentInitiatedMotoEnabled() {
        return agentInitiatedMotoEnabled;
    }

    public void setAgentInitiatedMotoEnabled(boolean agentInitiatedMotoEnabled) {
        this.agentInitiatedMotoEnabled = agentInitiatedMotoEnabled;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public ZonedDateTime getWentLiveDate() {
        return wentLiveDate;
    }

    public void setWentLiveDate(ZonedDateTime wentLiveDate) {
        this.wentLiveDate = wentLiveDate;
    }

    public PspTestAccountStage getCurrentPspTestAccountStage() {
        return currentPspTestAccountStage;
    }

    public ServiceEntity setCurrentPspTestAccountStage(PspTestAccountStage currentPspTestAccountStage) {
        this.currentPspTestAccountStage = currentPspTestAccountStage;
        return this;
    }

    public Service toService() {
        Service service = Service.from(id,
                externalId, 
                ServiceName.from(getServiceNames().values()),
                this.redirectToServiceImmediatelyOnTerminalState,
                this.collectBillingAddress,
                this.currentGoLiveStage,
                this.experimentalFeaturesEnabled,
                this.agentInitiatedMotoEnabled,
                this.sector,
                this.internal,
                this.archived,
                this.createdDate,
                this.wentLiveDate,
                this.currentPspTestAccountStage);
        service.setGatewayAccountIds(gatewayAccountIds.stream()
                .map(GatewayAccountIdEntity::getGatewayAccountId)
                .collect(Collectors.toList()));
        service.setCustomBranding(this.customBranding);
        if (this.merchantDetailsEntity != null) {
            service.setMerchantDetails(this.merchantDetailsEntity.toMerchantDetails());
        }
        return service;
    }

    public boolean hasExactGatewayAccountIds(List<String> gatewayAccountIds) {

        if (this.gatewayAccountIds.size() != gatewayAccountIds.size()) {
            return false;
        }

        for (GatewayAccountIdEntity gatewayAccountIdEntity : this.gatewayAccountIds) {
            if (!gatewayAccountIds.contains(gatewayAccountIdEntity.getGatewayAccountId())) {
                return false;
            }
        }

        return true;
    }

    public static ServiceEntity from(Service service) {
        ServiceEntity serviceEntity = new ServiceEntity();
        service.getServiceNames().forEach((languageCode, serviceName) ->
                serviceEntity.addOrUpdateServiceName(ServiceNameEntity.from(SupportedLanguage.fromIso639AlphaTwoCode(languageCode), serviceName)));
        serviceEntity.setExternalId(service.getExternalId());
        serviceEntity.setRedirectToServiceImmediatelyOnTerminalState(service.isRedirectToServiceImmediatelyOnTerminalState());
        serviceEntity.setCollectBillingAddress(service.isCollectBillingAddress());
        return serviceEntity;
    }

    public void addOrUpdateServiceName(ServiceNameEntity newServiceName) {
        newServiceName.setService(this);
        serviceNames.stream()
                .filter(serviceName -> serviceName.getLanguage().equals(newServiceName.getLanguage()))
                .findFirst()
                .ifPresentOrElse(
                        existingServiceName -> existingServiceName.setName(newServiceName.getName()),
                        () -> serviceNames.add(newServiceName)
                );
    }

    public Map<SupportedLanguage, ServiceNameEntity> getServiceNames() {
        return serviceNames.stream()
                .collect(Collectors.toMap(ServiceNameEntity::getLanguage, serviceName -> serviceName));
    }

    private void populateGatewayAccountIds(List<String> gatewayAccountIds) {
        for (String gatewayAccountId : gatewayAccountIds) {
            this.gatewayAccountIds.add(new GatewayAccountIdEntity(gatewayAccountId, this));
        }
    }
}
