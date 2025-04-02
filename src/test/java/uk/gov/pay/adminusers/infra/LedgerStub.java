package uk.gov.pay.adminusers.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import uk.gov.pay.adminusers.client.ledger.model.LedgerSearchTransactionsResponse;
import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class LedgerStub {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private WireMockExtension wireMockExtension;
    private WireMockServer wireMockServer;

    public LedgerStub(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    public LedgerStub(WireMockExtension wireMockExtension) {
        this.wireMockExtension = wireMockExtension;
    }

    public void returnLedgerTransaction(String externalId, LedgerTransaction ledgerTransaction) throws JsonProcessingException {
        ResponseDefinitionBuilder response = aResponse()
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withStatus(200)
                .withBody(objectMapper.writeValueAsString(ledgerTransaction));
        wireMockServer.stubFor(
                get(urlPathEqualTo(format("/v1/transaction/%s", externalId)))
                        .withQueryParam("override_account_id_restriction", equalTo("true"))
                        .willReturn(response)
        );
    }

    public void returnLedgerTransactionsForSearch(String gatewayAccountId, LedgerSearchTransactionsResponse ledgerResponse) throws JsonProcessingException {
        ResponseDefinitionBuilder response = aResponse()
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withStatus(200)
                .withBody(objectMapper.writeValueAsString(ledgerResponse));
        wireMockExtension.stubFor(
                get(urlPathEqualTo("/v1/transaction"))
                        .withQueryParam("account_id", equalTo(gatewayAccountId))
                        .withQueryParam("display_size", equalTo("1"))
                        .willReturn(response)
        );
    }
}
