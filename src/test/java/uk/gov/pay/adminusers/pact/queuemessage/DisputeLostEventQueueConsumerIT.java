package uk.gov.pay.adminusers.pact.queuemessage;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.MessagePactProviderRule;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;
import uk.gov.pay.adminusers.fixtures.EventFixture;
import uk.gov.pay.adminusers.fixtures.ServiceDbFixture;
import uk.gov.pay.adminusers.infra.AppWithPostgresAndSqsRule;
import uk.gov.pay.adminusers.infra.LedgerStub;
import uk.gov.pay.adminusers.infra.NotifyStub;
import uk.gov.pay.adminusers.infra.SqsTestDocker;
import uk.gov.pay.adminusers.model.MerchantDetails;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.dropwizard.testing.ConfigOverride.config;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static uk.gov.pay.adminusers.fixtures.EventFixture.anEventFixture;
import static uk.gov.pay.adminusers.fixtures.LedgerTransactionFixture.aLedgerTransactionFixture;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class DisputeLostEventQueueConsumerIT {

    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);

    @Rule
    public AppWithPostgresAndSqsRule adminusersApp = new AppWithPostgresAndSqsRule(
            config("eventSubscriberQueue.eventSubscriberQueueEnabled", "true")
    );

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().port(adminusersApp.getWireMockPort()));

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private byte[] currentMessage;
    private final String gatewayAccountId = "a-gateway-account-id";
    private final String resourceExternalId = "dispute-id";
    private final String parentResourceExternalId = "payment-id";
    private final String reference = "REF123";
    private final String serviceId = "service-id";
    private final String serviceName = "A service";
    private final String organisationName = "organisation name";
    ;
    private final String adminUserEmail = "user@example.com";

    private EventFixture eventFixture;

    private LedgerStub ledgerStub;
    private NotifyStub notifyStub;

    @Pact(provider = "connector", consumer = "adminusers")
    public MessagePact createEvidenceSubmittedEventPact(MessagePactBuilder builder) {
        JsonNode eventDetails = objectMapper.valueToTree(Map.of(
                "gateway_account_id", gatewayAccountId,
                "amount", 1000,
                "fee", 1500
        ));

        eventFixture = anEventFixture()
                .withLive(true)
                .withResourceExternalId(resourceExternalId)
                .withParentResourceExternalId(parentResourceExternalId)
                .withServiceId(serviceId)
                .withEventType("DISPUTE_LOST")
                .withEventDetails(eventDetails);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a dispute lost event")
                .withMetadata(metadata)
                .withContent(eventFixture.getAsPact())
                .toPact();
    }

    @Before
    public void setUp() throws Exception {
        adminusersApp.getDatabaseTestHelper().truncateAllData();

        ledgerStub = new LedgerStub(wireMockRule);
        notifyStub = new NotifyStub(wireMockRule);

        Service service = ServiceDbFixture.serviceDbFixture(adminusersApp.getDatabaseTestHelper())
                .withGatewayAccountIds(gatewayAccountId)
                .withName(serviceName)
                .withMerchantDetails(new MerchantDetails(
                        organisationName, "number", "line1", null, "city",
                        "postcode", "country", "dd-merchant@example.com",
                        "https://merchant.example.org"
                ))
                .insertService();
        Role adminRole = roleDbFixture(adminusersApp.getDatabaseTestHelper()).insertAdmin();
        userDbFixture(adminusersApp.getDatabaseTestHelper())
                .withEmail(adminUserEmail)
                .withServiceRole(service.getId(), adminRole.getId())
                .insertUser();

        LedgerTransaction ledgerTransaction = aLedgerTransactionFixture()
                .withTransactionId(parentResourceExternalId)
                .withReference(reference)
                .build();
        ledgerStub.returnLedgerTransaction(parentResourceExternalId, ledgerTransaction);
        notifyStub.stubSendEmail();
    }

    @Test
    @PactVerification({"connector"})
    public void test() throws Exception {
        String messageContents = new String(currentMessage);
        String snsMessage = new GsonBuilder().create().toJson(Map.of("Message", messageContents));
        adminusersApp.getSqsClient().sendMessage(SqsTestDocker.getQueueUrl("event-queue"), snsMessage);
        await().atMost(2, TimeUnit.SECONDS).until(
                () -> !wireMockRule.findAll(RequestPatternBuilder.newRequestPattern().withUrl("/v2/notifications/email")).isEmpty());

        wireMockRule.verify(1, postRequestedFor(urlPathEqualTo("/v2/notifications/email"))
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON))
                .withRequestBody(matchingJsonPath("$.email_address", equalTo(adminUserEmail)))
                .withRequestBody(matchingJsonPath("$.template_id", equalTo("pay-notify-stripe-dispute-lost-email-template-id")))
                .withRequestBody(matchingJsonPath("$.email_reply_to_id", equalTo("pay-notify-email-reply-to-support-id")))
                .withRequestBody(matchingJsonPath("$.personalisation.serviceName", equalTo(serviceName)))
                .withRequestBody(matchingJsonPath("$.personalisation.organisationName", equalTo(organisationName)))
                .withRequestBody(matchingJsonPath("$.personalisation.disputedAmount", equalTo("10.00")))
                .withRequestBody(matchingJsonPath("$.personalisation.disputeFee", equalTo("15.00")))

        );
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
