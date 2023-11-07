package uk.gov.pay.adminusers.client.ledger.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LedgerSearchTransactionsResponse {

    @JsonProperty("results")
    private List<LedgerTransaction> transactions;

    public LedgerSearchTransactionsResponse(){
        //empty constructor
    }
    public LedgerSearchTransactionsResponse(List<LedgerTransaction> transactions) {
        this.transactions = transactions;
    }

    public List<LedgerTransaction> getTransactions() {
        return transactions;
    }

}
