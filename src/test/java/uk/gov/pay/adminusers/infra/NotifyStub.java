package uk.gov.pay.adminusers.infra;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public class NotifyStub {
    
    private final WireMockServer wireMockServer;

    public NotifyStub(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    public void stubSendEmail() {
        wireMockServer.stubFor(
                post(urlPathEqualTo("/v2/notifications/email"))
                        .willReturn(aResponse()
                                .withStatus(200))
        );
    }
}
