package uk.gov.pay.adminusers.queue.model.event;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DisputeCreatedDetails {
    
    @JsonProperty("amount")
    private Long paymentAmount;
    @JsonProperty("fee")
    private Long disputeFee;
    @JsonProperty("evidence_due_date")
    private Long disputeEvidenceDueDate;
    
    public DisputeCreatedDetails() {
        // empty constructor
    }

    public DisputeCreatedDetails(Long paymentAmount, Long disputeFee, Long disputeEvidenceDueDate) {
        this.paymentAmount = paymentAmount;
        this.disputeFee = disputeFee;
        this.disputeEvidenceDueDate = disputeEvidenceDueDate;
    }

    public Long getPaymentAmount() {
        return paymentAmount;
    }

    public Long getDisputeFee() {
        return disputeFee;
    }

    public long getDisputeEvidenceDueDate() {
        return disputeEvidenceDueDate;
    }
}
