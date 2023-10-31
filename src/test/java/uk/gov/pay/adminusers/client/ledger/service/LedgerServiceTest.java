package uk.gov.pay.adminusers.client.ledger.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.client.ledger.exception.LedgerException;
import uk.gov.pay.adminusers.client.ledger.model.LedgerSearchTransactionsResponse;
import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LedgerServiceTest {

    @Mock
    AdminUsersConfig mockConfiguration;

    @Mock
    private Client mockClient;

    @Mock
    private WebTarget mockWebTarget;

    @Mock
    private Invocation.Builder mockInvocationBuilder;

    @Mock
    private Response mockResponse;

    private LedgerService serviceUnderTest;

    private static final String LEDGER_URL = "http://ledgerUrl";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        when(mockConfiguration.getLedgerBaseUrl()).thenReturn(LEDGER_URL);
        when(mockClient.target(any(UriBuilder.class))).thenReturn(mockWebTarget);
        when(mockWebTarget.request()).thenReturn(mockInvocationBuilder);
        when(mockInvocationBuilder.accept(APPLICATION_JSON)).thenReturn(mockInvocationBuilder);
        when(mockInvocationBuilder.get()).thenReturn(mockResponse);
        when(mockResponse.getStatus()).thenReturn(SC_OK);
        serviceUnderTest = new LedgerService(mockClient, mockConfiguration);
    }

    @Test
    public void getTransaction_shouldDeserialiseLedgerPaymentTransactionCorrectly() throws JsonProcessingException {
        String externalId = "e8eq11mi2ndmauvb51qsg8hccn";
        ImmutableMap<String, String> transactionData = ImmutableMap.of("transaction_id", externalId, "reference", "test event ref", "foo", "bar");
        String ledgerPayload = new Gson().toJson(transactionData);
        when(mockResponse.readEntity(LedgerTransaction.class)).thenReturn(objectMapper.readValue(ledgerPayload, LedgerTransaction.class));
        Optional<LedgerTransaction> mayBeTransaction = serviceUnderTest.getTransaction(externalId);
        assertThat(mayBeTransaction.isPresent(), is(true));
        LedgerTransaction transaction = mayBeTransaction.get();
        assertThat(transaction.getTransactionId(), is(externalId));
        assertThat(transaction.getReference(), is("test event ref"));
    }

    @Test
    public void searchTransactions_shouldDeserialiseSearchTransactionsResponseCorrectly() throws JsonProcessingException {
        String externalId = "e8eq11mi2ndmauvb51qsg8hccn";
        ImmutableMap<String, Object> transactionData = ImmutableMap.of(
                "total", 1,
                "count", 1,
                "results", List.of(
                        Map.of("reference", "ref-1", "created_date", "2023-10-09T16:31:13.511Z",
                                "transaction_id", externalId, "gateway_account_id", "1")
                ));
        String ledgerPayload = new Gson().toJson(transactionData);

        when(mockResponse.readEntity(LedgerSearchTransactionsResponse.class)).thenReturn(objectMapper.readValue(ledgerPayload, LedgerSearchTransactionsResponse.class));
        LedgerSearchTransactionsResponse ledgerSearchTransactionsResponse = serviceUnderTest.searchTransactions("1", 1);
        assertThat(ledgerSearchTransactionsResponse.getTransactions().size(), is(1));
        assertThat(ledgerSearchTransactionsResponse.getTransactions().get(0).getCreatedDate().toString(), is("2023-10-09T16:31:13.511Z"));
        assertThat(ledgerSearchTransactionsResponse.getTransactions().get(0).getTransactionId(), is("e8eq11mi2ndmauvb51qsg8hccn"));
    }

    @Test
    public void searchTransactions_shouldTHrowExceptionIfLedgerReturnsNon2xxError() throws JsonProcessingException {
        when(mockResponse.getStatus()).thenReturn(SC_INTERNAL_SERVER_ERROR);

        assertThrows(LedgerException.class, () -> serviceUnderTest.searchTransactions("1", 1));
    }
}
