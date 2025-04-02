package uk.gov.pay.adminusers.client.ledger.service;

import au.com.dius.pact.consumer.junit.PactVerification;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.app.RestClientFactory;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.app.config.RestClientConfig;
import uk.gov.pay.adminusers.client.ledger.model.LedgerSearchTransactionsResponse;
import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;
import uk.gov.service.payments.commons.testing.pact.consumers.Pacts;
import uk.gov.service.payments.commons.testing.pact.consumers.PayPactProviderRule;

import javax.ws.rs.client.Client;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LedgerServiceConsumerTest {

    @Rule
    public PayPactProviderRule ledgerRule = new PayPactProviderRule("ledger", this);

    @Mock
    AdminUsersConfig configuration;
    
    private LedgerService ledgerService;

    @Before
    public void setUp() {
        when(configuration.getLedgerBaseUrl()).thenReturn(ledgerRule.getUrl());
        Client client = RestClientFactory.buildClient(new RestClientConfig());
        ledgerService = new LedgerService(client, configuration);
    }

    @Test
    @PactVerification("ledger")
    @Pacts(pacts = {"adminusers-ledger-get-payment-transaction"})
    public void getTransaction_shouldSerialiseLedgerPaymentTransactionCorrectly() {
        String externalId = "e8eq11mi2ndmauvb51qsg8hccn";
        Optional<LedgerTransaction> mayBeTransaction = ledgerService.getTransaction(externalId);

        assertThat(mayBeTransaction.isPresent(), is(true));
        LedgerTransaction transaction = mayBeTransaction.get();
        assertThat(transaction.getTransactionId(), is(externalId));
        assertThat(transaction.getReference(), is(notNullValue()));
    }

    @Test
    @PactVerification("ledger")
    @Pacts(pacts = {"adminusers-ledger-search-transactions"})
    public void searchTransactions_shouldDeserialiseLedgerResponseCorrectly() {
        String externalId = "e8eq11m3f2bg32dlll0dl2kdjg";
        LedgerSearchTransactionsResponse searchTransactionsResponse = ledgerService.searchTransactions("54322", 1);

        assertThat(searchTransactionsResponse.getTransactions().size(), is(1));
        LedgerTransaction transaction = searchTransactionsResponse.getTransactions().get(0);
        assertThat(transaction.getTransactionId(), is(externalId));
        assertThat(transaction.getCreatedDate(), is(notNullValue()));
    }
}
