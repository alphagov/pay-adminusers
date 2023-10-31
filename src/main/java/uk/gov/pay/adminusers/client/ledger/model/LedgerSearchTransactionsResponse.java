package uk.gov.pay.adminusers.client.ledger.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LedgerSearchTransactionsResponse {

    @JsonProperty("total")
    private int total;

    @JsonProperty("count")
    private int count;

    @JsonProperty("results")
    private List<LedgerTransaction> transactions;

    public List<LedgerTransaction> getTransactions() {
        return transactions;
    }

    public int getTotal() {
        return total;
    }

    public int getCount() {
        return count;
    }
}
