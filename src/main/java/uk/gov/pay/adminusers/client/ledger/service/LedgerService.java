package uk.gov.pay.adminusers.client.ledger.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.client.ledger.exception.LedgerException;
import uk.gov.pay.adminusers.client.ledger.model.LedgerSearchTransactionsResponse;
import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.util.Optional;

import static java.lang.String.format;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.service.payments.logging.LoggingKeys.GATEWAY_ACCOUNT_ID;

public class LedgerService {

    private final Logger logger = LoggerFactory.getLogger(LedgerService.class);

    private final Client client;
    private final String ledgerUrl;

    @Inject
    public LedgerService(Client client, AdminUsersConfig configuration) {
        this.client = client;
        this.ledgerUrl = configuration.getLedgerBaseUrl();
    }

    public Optional<LedgerTransaction> getTransaction(String id) {
        var uri = UriBuilder
                .fromPath(ledgerUrl)
                .path(format("/v1/transaction/%s", id))
                .queryParam("override_account_id_restriction", "true");
        logger.info("Querying ledger for transaction: {}", id);
        return getTransactionFromLedger(uri);
    }

    public LedgerSearchTransactionsResponse searchTransactions(String gatewayAccountId, int limit) {
        var uri = UriBuilder
                .fromPath(ledgerUrl)
                .path("/v1/transaction")
                .queryParam("account_id", gatewayAccountId)
                .queryParam("display_size", limit);

        logger.info("Searching transactions",
                kv(GATEWAY_ACCOUNT_ID, gatewayAccountId));

        Response response = getResponse(uri);

        if (response.getStatus() == SC_OK) {
            return response.readEntity(LedgerSearchTransactionsResponse.class);
        }

        throw new LedgerException(response);
    }

    private Optional<LedgerTransaction> getTransactionFromLedger(UriBuilder uri) {
        Response response = getResponse(uri);

        if (response.getStatus() == SC_OK) {
            return Optional.of(response.readEntity(LedgerTransaction.class));
        }

        return Optional.empty();
    }

    private Response getResponse(UriBuilder uri) {
        return client
                .target(uri)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
    }

}
