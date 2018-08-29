package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.apache.commons.lang3.StringUtils;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.pay.commons.model.SupportedLanguage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Service {

    public static String DEFAULT_NAME_VALUE = "System Generated";

    private Integer id;
    private String externalId;
    private String name = DEFAULT_NAME_VALUE;
    private List<Link> links = new ArrayList<>();
    private List<String> gatewayAccountIds = new ArrayList<>();
    private Map<String, Object> customBranding;
    private MerchantDetails merchantDetails;
    private Map<String, String> serviceNames;

    public static Service from() {
        return from(DEFAULT_NAME_VALUE);
    }

    public static Service from(String name) {
        return from(randomInt(), randomUuid(), name);
    }

    public static Service from(String name, Map<SupportedLanguage, ServiceNameEntity> multilingualServiceNames) {
        return from(randomInt(), randomUuid(), name, multilingualServiceNames);
    }

    public static Service from(Integer id, String externalId, String name) {
        return new Service(id, externalId, name, Collections.emptyMap());
    }

    public static Service from(Integer id, String externalId, String name, Map<SupportedLanguage, ServiceNameEntity> multilingualServiceNames) {
        return new Service(id, externalId, name, multilingualServiceNames);
    }

    private Service(@JsonProperty("id") Integer id,
                    @JsonProperty("external_id") String externalId,
                    @JsonProperty("name") String name,
                    Map<SupportedLanguage, ServiceNameEntity> multilingualServiceNames) {
        this.id = id;
        this.externalId = externalId;
        this.name = name;

        this.serviceNames = new LinkedHashMap<>();
        serviceNames.put(SupportedLanguage.ENGLISH.toString(), name);
        multilingualServiceNames.entrySet().stream()
                .filter(entry -> StringUtils.isNotBlank(entry.getValue().getName()))
                .forEach(entry -> serviceNames.put(entry.getKey().toString(), entry.getValue().getName()));
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
            customBranding = null;
        }
        this.customBranding = customBranding;
    }

    public MerchantDetails getMerchantDetails() {
        return merchantDetails;
    }

    public void setMerchantDetails(MerchantDetails merchantDetails) {
        this.merchantDetails = merchantDetails;
    }

    @JsonProperty("service_name")
    public Map<String, String> getServiceNames() {
        return serviceNames;
    }

}
