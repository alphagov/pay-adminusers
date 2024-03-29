package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;

import java.time.ZonedDateTime;

public class LedgerTransactionFixture {

    private String transactionId;
    private String reference;
    private ZonedDateTime createdDate;

    private LedgerTransactionFixture() {
    }

    public static LedgerTransactionFixture aLedgerTransactionFixture() {
        return new LedgerTransactionFixture();
    }


    public LedgerTransactionFixture withTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public LedgerTransactionFixture withReference(String reference) {
        this.reference = reference;
        return this;
    }

    public LedgerTransactionFixture withCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public LedgerTransaction build() {
        return new LedgerTransaction(
                transactionId,
                reference,
                createdDate
        );
    }
    
}
