package uk.gov.pay.adminusers.client.ledger.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.service.payments.commons.api.json.ApiResponseDateTimeDeserializer;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LedgerTransaction {

    private String transactionId;
    private String reference;

    @JsonDeserialize(using = ApiResponseDateTimeDeserializer.class)
    private ZonedDateTime createdDate;

    public LedgerTransaction() {
        // empty constructor
    }

    public LedgerTransaction(String transactionId, String reference) {
        this.transactionId = transactionId;
        this.reference = reference;
    }


    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }
}
