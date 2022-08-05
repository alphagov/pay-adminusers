package uk.gov.pay.adminusers.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
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
    private final WireMockServer wireMockServer;

    public LedgerStub(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
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
}
