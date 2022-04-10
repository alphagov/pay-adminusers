package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.service.payments.commons.api.json.ApiResponseDateTimeSerializer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.GoLiveStage.NOT_STARTED;
import static uk.gov.pay.adminusers.persistence.entity.ServiceEntity.DEFAULT_BILLING_ADDRESS_COUNTRY;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Service {

    public static final String DEFAULT_NAME_VALUE = "System Generated";

    private Integer id;
    @Schema(example = "7d19aff33f8948deb97ed16b2912dcd3")
    private String externalId;
    private List<Link> links = new ArrayList<>();
    @ArraySchema(schema = @Schema(example = "1"))
    private List<String> gatewayAccountIds = new ArrayList<>();
    @Schema(example = "{" +
            "\"image_url\": \"image url\"," +
            "\"css_url\": \"css url\"" +
            "}")
    private Map<String, Object> customBranding;
    private MerchantDetails merchantDetails;
    private boolean redirectToServiceImmediatelyOnTerminalState;
    private boolean collectBillingAddress;
    private String defaultBillingAddressCountry;
    private GoLiveStage goLiveStage;
    @Schema(example = "false")
    private boolean experimentalFeaturesEnabled;
    @Schema(example = "local government")
    private String sector;
    @Schema(example = "false")
    private boolean internal;
    @Schema(example = "false")
    private boolean archived;
    @Schema(example = "false")
    private boolean agentInitiatedMotoEnabled;
    private PspTestAccountStage currentPspTestAccountStage;

    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    @Schema(example = "2022-04-01T12:07:46.568Z")
    private ZonedDateTime createdDate;

    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    @Schema(example = "2022-04-09T18:07:46.568Z")
    private ZonedDateTime wentLiveDate;

    @JsonIgnore
    private ServiceName serviceName;

    public static Service from() {
        return from(new ServiceName(DEFAULT_NAME_VALUE));
    }

    public static Service from(ServiceName serviceName) {
        return from(randomInt(), randomUuid(), serviceName);
    }

    public static Service from(Integer id, String externalId, ServiceName serviceName) {
        return from(id,
                externalId,
                serviceName,
                false,
                true,
                DEFAULT_BILLING_ADDRESS_COUNTRY,
                NOT_STARTED,
                false,
                false,
                null,
                false,
                false,
                null,
                null,
                null);
    }

    public static Service from(Integer id,
                               String externalId,
                               ServiceName serviceName,
                               boolean redirectToServiceImmediatelyOnTerminalState,
                               boolean collectBillingAddress,
                               String defaultBillingAddressCountry,
                               GoLiveStage goLiveStage,
                               boolean experimentalFeaturesEnabled,
                               boolean agentInitiatedMotoEnabled,
                               String sector,
                               boolean internal,
                               boolean archived,
                               ZonedDateTime createdDate,
                               ZonedDateTime wentLiveDate,
                               PspTestAccountStage pspTestAccountStage) {
        return new Service(id,
                externalId,
                serviceName,
                redirectToServiceImmediatelyOnTerminalState,
                collectBillingAddress,
                defaultBillingAddressCountry,
                goLiveStage,
                experimentalFeaturesEnabled,
                agentInitiatedMotoEnabled,
                sector,
                internal,
                archived,
                createdDate,
                wentLiveDate,
                pspTestAccountStage);
    }

    private Service(@JsonProperty("id") Integer id,
                    @JsonProperty("external_id") String externalId,
                    ServiceName serviceName,
                    boolean redirectToServiceImmediatelyOnTerminalState,
                    boolean collectBillingAddress,
                    String defaultBillingAddressCountry,
                    GoLiveStage goLiveStage,
                    boolean experimentalFeaturesEnabled,
                    boolean agentInitiatedMotoEnabled,
                    String sector,
                    boolean internal,
                    boolean archived,
                    ZonedDateTime createdDate,
                    ZonedDateTime wentLiveDate,
                    PspTestAccountStage currentPspTestAccountStage) {
        this.id = id;
        this.externalId = externalId;
        this.redirectToServiceImmediatelyOnTerminalState = redirectToServiceImmediatelyOnTerminalState;
        this.collectBillingAddress = collectBillingAddress;
        this.defaultBillingAddressCountry = defaultBillingAddressCountry;
        this.serviceName = serviceName;
        this.goLiveStage = goLiveStage;
        this.experimentalFeaturesEnabled = experimentalFeaturesEnabled;
        this.agentInitiatedMotoEnabled = agentInitiatedMotoEnabled;
        this.sector = sector;
        this.internal = internal;
        this.archived = archived;
        this.createdDate = createdDate;
        this.wentLiveDate = wentLiveDate;
        this.currentPspTestAccountStage = currentPspTestAccountStage;
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
    @Schema(example = "Some service name")
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
    @Schema(example = " {" +
            "  \"en\": \"Some service name\"," +
            "  \"cy\": \"Service name in welsh\"" +
            "    }")
    public Map<String, String> getServiceNames() {
        return serviceName.getEnglishAndTranslations().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(languageToName -> languageToName.getKey().toString(), Map.Entry::getValue));
    }

    @JsonProperty("redirect_to_service_immediately_on_terminal_state")
    @Schema(example = "false")
    public boolean isRedirectToServiceImmediatelyOnTerminalState() {
        return redirectToServiceImmediatelyOnTerminalState;
    }

    public void setRedirectToServiceImmediatelyOnTerminalState(boolean redirectToServiceImmediatelyOnTerminalState) {
        this.redirectToServiceImmediatelyOnTerminalState = redirectToServiceImmediatelyOnTerminalState;
    }

    @JsonProperty("collect_billing_address")
    @Schema(example = "true")
    public boolean isCollectBillingAddress() {
        return collectBillingAddress;
    }

    public void setCollectBillingAddress(boolean collectBillingAddress) {
        this.collectBillingAddress = collectBillingAddress;
    }

    @JsonProperty("default_billing_address_country")
    @Schema(example = "GB")
    public String getDefaultBillingAddressCountry() {
        return defaultBillingAddressCountry;
    }

    public void setDefaultBillingAddressCountry(String defaultBillingAddressCountry) {
        this.defaultBillingAddressCountry = defaultBillingAddressCountry;
    }

    @JsonProperty("current_go_live_stage")
    @Schema(example = "NOT_STARTED")
    public GoLiveStage getGoLiveStage() {
        return goLiveStage;
    }

    public void setGoLiveStage(GoLiveStage goLiveStage) {
        this.goLiveStage = goLiveStage;
    }

    @JsonProperty("current_psp_test_account_stage")
    @Schema(example = "NOT_STARTED")
    public PspTestAccountStage getCurrentPspTestAccountStage() {
        return currentPspTestAccountStage;
    }

    public void setCurrentPspTestAccountStage(PspTestAccountStage currentPspTestAccountStage) {
        this.currentPspTestAccountStage = currentPspTestAccountStage;
    }

    @JsonProperty("experimental_features_enabled")
    public boolean isExperimentalFeaturesEnabled() {
        return experimentalFeaturesEnabled;
    }

    public void setExperimentalFeaturesEnabled(boolean experimentalFeaturesEnabled) {
        this.experimentalFeaturesEnabled = experimentalFeaturesEnabled;
    }

    @JsonProperty("agent_initiated_moto_enabled")
    public boolean isAgentInitiatedMotoEnabled() {
        return agentInitiatedMotoEnabled;
    }

    public void setAgentInitiatedMotoEnabled(boolean agentInitiatedMotoEnabled) {
        this.agentInitiatedMotoEnabled = agentInitiatedMotoEnabled;
    }

    public String getSector() {
        return sector;
    }

    public boolean isInternal() {
        return internal;
    }

    public boolean isArchived() {
        return archived;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public ZonedDateTime getWentLiveDate() {
        return wentLiveDate;
    }
}
