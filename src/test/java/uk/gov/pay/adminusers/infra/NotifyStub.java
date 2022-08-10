package uk.gov.pay.adminusers.infra;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.gson.GsonBuilder;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public class NotifyStub {
    
    private final WireMockServer wireMockServer;

    public NotifyStub(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    public void stubSendEmail() {
        
        String responseBody = (new GsonBuilder()).create().toJson(Map.of("id", "d2c1a8d1-b897-4013-b761-f2b442ebdc10",
                "reference", "a-reference",
                "content", Map.of(
                        "body", "body",
                        "subject", "subject"
                ),
                "template", Map.of(
                        "id", "1d2ce804-b51c-4108-a306-1789bd2699e0",
                        "version", 1,
                        "uri", "template-uri")));
        
        wireMockServer.stubFor(
                post(urlPathEqualTo("/v2/notifications/email"))
                        .willReturn(aResponse()
                                .withStatus(201)
                                .withBody(responseBody))
        );
    }
}
