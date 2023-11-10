package uk.gov.pay.adminusers.infra;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public class ConnectorTaskQueueStub {
    private WireMockExtension wireMockExtension;

    public ConnectorTaskQueueStub(WireMockExtension wireMockExtension) {
        this.wireMockExtension = wireMockExtension;
    }

    public void returnOkWhenPlacingTasksOnQueue() {
        wireMockExtension.stubFor(post(urlPathEqualTo("")).willReturn(aResponse().withStatus(200)));
    }
}
