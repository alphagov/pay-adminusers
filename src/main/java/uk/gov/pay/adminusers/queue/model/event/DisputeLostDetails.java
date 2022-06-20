package uk.gov.pay.adminusers.queue.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DisputeLostDetails {
    private Long netAmount;
    private Long amount;
    private Long fee;
    private String gatewayAccountId;

    public DisputeLostDetails() {
        // empty constructor
    }

    public Long getNetAmount() {
        return netAmount;
    }

    public Long getAmount() {
        return amount;
    }

    public Long getFee() {
        return fee;
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }
}
