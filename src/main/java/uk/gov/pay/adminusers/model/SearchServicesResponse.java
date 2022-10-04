package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SearchServicesResponse {
    private final List<Service> nameResults;
    
    private final List<Service> merchantResults;

    public SearchServicesResponse(List<Service> nameResults, List<Service> merchantResults) {
        this.nameResults = nameResults;
        this.merchantResults = merchantResults;
    }

    public List<Service> getNameResults() {
        return nameResults;
    }

    public List<Service> getMerchantResults() {
        return merchantResults;
    }
}
