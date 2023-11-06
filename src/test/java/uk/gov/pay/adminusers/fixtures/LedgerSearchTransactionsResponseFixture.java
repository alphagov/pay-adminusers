package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.client.ledger.model.LedgerSearchTransactionsResponse;
import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;

import java.util.List;

public class LedgerSearchTransactionsResponseFixture {

    List<LedgerTransaction> transactionList;

    public static LedgerSearchTransactionsResponseFixture aLedgerSearchTransactionsResponseFixture() {
        return new LedgerSearchTransactionsResponseFixture();
    }

    public LedgerSearchTransactionsResponseFixture withTransactionList(List<LedgerTransaction> transactions) {
        this.transactionList = transactions;
        return this;
    }

    public LedgerSearchTransactionsResponse build() {
        return new LedgerSearchTransactionsResponse(transactionList);
    }
}
