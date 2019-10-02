package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.GoLiveStage.NOT_STARTED;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Service {

    public static String DEFAULT_NAME_VALUE = "System Generated";

    private Integer id;
    private String externalId;
    private List<Link> links = new ArrayList<>();
    private List<String> gatewayAccountIds = new ArrayList<>();
    private Map<String, Object> customBranding;
    private MerchantDetails merchantDetails;
    private boolean redirectToServiceImmediatelyOnTerminalState;
    private boolean collectBillingAddress;
    private GoLiveStage goLiveStage;

    @JsonIgnore
    private ServiceName serviceName;

    public static Service from() {
        return from(new ServiceName(DEFAULT_NAME_VALUE));
    }

    public static Service from(ServiceName serviceName) {
        return from(randomInt(), randomUuid(), serviceName);
    }

    public static Service from(Integer id, String externalId, ServiceName serviceName) {
        return from(id, externalId, serviceName, false, true, NOT_STARTED);
    }

    public static Service from(Integer id,
                               String externalId,
                               ServiceName serviceName,
                               boolean redirectToServiceImmediatelyOnTerminalState,
                               boolean collectBillingAddress,
                               GoLiveStage goLiveStage) {
        return new Service(id, externalId, serviceName, redirectToServiceImmediatelyOnTerminalState, collectBillingAddress, goLiveStage);
    }

    private Service(@JsonProperty("id") Integer id,
                    @JsonProperty("external_id") String externalId,
                    ServiceName serviceName,
                    boolean redirectToServiceImmediatelyOnTerminalState,
                    boolean collectBillingAddress,
                    GoLiveStage goLiveStage) {
        this.id = id;
        this.externalId = externalId;
        this.redirectToServiceImmediatelyOnTerminalState = redirectToServiceImmediatelyOnTerminalState;
        this.collectBillingAddress = collectBillingAddress;
        this.serviceName = serviceName;
        this.goLiveStage = goLiveStage;
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

    @JsonProperty("name")
    public String getName() {
        return serviceName.getEnglish();
    }

    @JsonProperty("_links")
    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public List<String> getGatewayAccountIds() {
        return gatewayAccountIds;
    }

    public void setGatewayAccountIds(List<String> gatewayAccountIds) {
        this.gatewayAccountIds = gatewayAccountIds;
    }

    public Map<String, Object> getCustomBranding() {
        return customBranding;
    }

    /**
     * nullify if map is empty, so that it will be undefined in response
     *
     * @param customBranding
     */
    public void setCustomBranding(Map<String, Object> customBranding) {
        if (customBranding != null && customBranding.isEmpty()) {
            this.customBranding = null;
        } else {
            this.customBranding = customBranding;
        }
    }

    public MerchantDetails getMerchantDetails() {
        return merchantDetails;
    }

    public void setMerchantDetails(MerchantDetails merchantDetails) {
        this.merchantDetails = merchantDetails;
    }

    @JsonProperty("service_name")
    public Map<String, String> getServiceNames() {
        return serviceName.getEnglishAndTranslations().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(languageToName -> languageToName.getKey().toString(), Map.Entry::getValue));
    }

    @JsonProperty("redirect_to_service_immediately_on_terminal_state")
    public boolean isRedirectToServiceImmediatelyOnTerminalState() {
        return redirectToServiceImmediatelyOnTerminalState;
    }

    public void setRedirectToServiceImmediatelyOnTerminalState(boolean redirectToServiceImmediatelyOnTerminalState) {
        this.redirectToServiceImmediatelyOnTerminalState = redirectToServiceImmediatelyOnTerminalState;
    }

    @JsonProperty("collect_billing_address")
    public boolean isCollectBillingAddress() {
        return collectBillingAddress;
    }

    public void setCollectBillingAddress(boolean collectBillingAddress) {
        this.collectBillingAddress = collectBillingAddress;
    }

    @JsonProperty("current_go_live_stage")
    public GoLiveStage getGoLiveStage() {
        return goLiveStage;
    }

    public void setGoLiveStage(GoLiveStage goLiveStage) {
        this.goLiveStage = goLiveStage;
    }
}
