package uk.gov.pay.adminusers.queue.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DisputeWonDetails {
    private String gatewayAccountId;

    public DisputeWonDetails() {
        // empty constructor
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }
}
