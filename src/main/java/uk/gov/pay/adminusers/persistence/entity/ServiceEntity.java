package uk.gov.pay.adminusers.persistence.entity;

import com.google.common.collect.ImmutableList;
import uk.gov.pay.adminusers.model.GoLiveStage;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.pay.commons.model.SupportedLanguage;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
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

    @Column(name = "name")
    private String name = Service.DEFAULT_NAME_VALUE;

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
    @Enumerated(value = EnumType.STRING)
    private GoLiveStage currentGoLiveStage = GoLiveStage.NOT_STARTED;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        return ImmutableList.copyOf(this.gatewayAccountIds);
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

    public Service toService() {
        Service service = Service.from(id, externalId, name, getServiceNames(), this.redirectToServiceImmediatelyOnTerminalState, this.collectBillingAddress, this.currentGoLiveStage);
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
        serviceEntity.setName(service.getName());
        serviceEntity.setExternalId(service.getExternalId());
        serviceEntity.setRedirectToServiceImmediatelyOnTerminalState(service.isRedirectToServiceImmediatelyOnTerminalState());
        serviceEntity.setCollectBillingAddress(service.isCollectBillingAddress());
        return serviceEntity;
    }

    public void addOrUpdateServiceName(ServiceNameEntity newServiceName) {
        if (newServiceName.getLanguage().equals(SupportedLanguage.ENGLISH)) {
            setName(newServiceName.getName());
        }
        newServiceName.setService(this);
        final Optional<ServiceNameEntity> existingServiceName = serviceNames.stream()
                .filter(n -> n.getLanguage().equals(newServiceName.getLanguage()))
                .findFirst();
        if (existingServiceName.isPresent()) {
            existingServiceName.get().setName(newServiceName.getName());
        } else {
            serviceNames.add(newServiceName);
        }
    }

    public Map<SupportedLanguage, ServiceNameEntity> getServiceNames() {
        return serviceNames.stream()
                .collect(Collectors.toMap(ServiceNameEntity::getLanguage, serviceName -> serviceName));
    }

    public GoLiveStage getCurrentGoLiveStage() {
        return currentGoLiveStage;
    }

    public void setCurrentGoLiveStage(GoLiveStage currentGoLiveStage) {
        this.currentGoLiveStage = currentGoLiveStage;
    }

    private void populateGatewayAccountIds(List<String> gatewayAccountIds) {
        for (String gatewayAccountId : gatewayAccountIds) {
            this.gatewayAccountIds.add(new GatewayAccountIdEntity(gatewayAccountId, this));
        }
    }
}
