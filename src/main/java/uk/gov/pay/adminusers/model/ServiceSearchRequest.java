package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Optional;

public class ServiceSearchRequest {

    private static final String FIELD_SERVICE_NAME = "service_name";
    private static final String FIELD_SERVICE_MERCHANT_NAME = "service_merchant_name";

    private final String serviceNameSearchString;
    private final String serviceMerchantNameSearchString;

    public ServiceSearchRequest(String serviceName, String serviceMerchantName) {
        this.serviceNameSearchString = serviceName;
        this.serviceMerchantNameSearchString = serviceMerchantName;
    }

    public static ServiceSearchRequest from(JsonNode payload) {
        String serviceName = Optional.ofNullable(payload.get(FIELD_SERVICE_NAME))
                .map(JsonNode::asText)
                .orElse("");
        String serviceOrg = Optional.ofNullable(payload.get(FIELD_SERVICE_MERCHANT_NAME))
                .map(JsonNode::asText)
                .orElse("");
        return new ServiceSearchRequest(serviceName, serviceOrg);
    }

    public String getServiceNameSearchString() {
        return serviceNameSearchString;
    }

    public String getServiceMerchantNameSearchString() {
        return serviceMerchantNameSearchString;
    }

    public Map<String, String> toMap() {
        return Map.of(FIELD_SERVICE_NAME, serviceNameSearchString, FIELD_SERVICE_MERCHANT_NAME, serviceMerchantNameSearchString);
    }
}
