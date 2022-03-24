package uk.gov.pay.adminusers.queue.model.event;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DisputeCreatedDetails {
    
    private Long amount;
    private Long fee;
    private Long evidenceDueDate;
    private String gatewayAccountId;
    
    public DisputeCreatedDetails() {
        // empty constructor
    }

    public Long getAmount() {
        return amount;
    }

    public Long getFee() {
        return fee;
    }

    public long getEvidenceDueDate() {
        return evidenceDueDate;
    }
    
    public String getGatewayAccountId() { return gatewayAccountId; }
}
