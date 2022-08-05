package uk.gov.pay.adminusers.queue.event;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;
import uk.gov.pay.adminusers.client.ledger.service.LedgerService;
import uk.gov.pay.adminusers.model.MerchantDetails;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceName;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.queue.model.Event;
import uk.gov.pay.adminusers.queue.model.EventMessage;
import uk.gov.pay.adminusers.queue.model.EventType;
import uk.gov.pay.adminusers.service.NotificationService;
import uk.gov.pay.adminusers.service.ServiceFinder;
import uk.gov.pay.adminusers.service.UserServices;
import uk.gov.service.payments.commons.queue.exception.QueueException;
import uk.gov.service.payments.commons.queue.model.QueueMessage;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.EventFixture.anEventFixture;
import static uk.gov.pay.adminusers.fixtures.LedgerTransactionFixture.aLedgerTransactionFixture;
import static uk.gov.pay.adminusers.model.Service.DEFAULT_NAME_VALUE;
import static uk.gov.pay.adminusers.service.UserServicesTest.aUserEntityWithRoleForService;

@ExtendWith(MockitoExtension.class)
class EventMessageHandlerTest {

    @Mock
    private EventSubscriberQueue mockEventSubscriberQueue;

    @Mock
    private NotificationService mockNotificationService;

    @Mock
    private ServiceFinder mockServiceFinder;

    @Mock
    private UserServices mockUserServices;

    @Mock
    private LedgerService mockLedgerService;

    @Captor
    ArgumentCaptor<Set<String>> adminEmailsCaptor;

    @Captor
    ArgumentCaptor<Map<String, String>> personalisationCaptor;
    @Mock
    private Appender<ILoggingEvent> mockLogAppender;
    @Captor
    ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String gatewayAccountId = "123";

    private EventMessageHandler eventMessageHandler;
    private Service service;
    private LedgerTransaction transaction;
    private List<UserEntity> users;
    private Event disputeEvent;

    @BeforeEach
    void setUp() {
        eventMessageHandler = new EventMessageHandler(mockEventSubscriberQueue, mockLedgerService, mockNotificationService, mockServiceFinder, mockUserServices, objectMapper);
        service = Service.from(randomInt(), randomUuid(), new ServiceName(DEFAULT_NAME_VALUE));
        service.setMerchantDetails(new MerchantDetails("Organisation Name", null, null, null, null, null, null, null, null));
        transaction = aLedgerTransactionFixture()
                .withTransactionId("456")
                .withReference("tx ref")
                .build();
        users = Arrays.asList(
                aUserEntityWithRoleForService(service, true, "admin1"),
                aUserEntityWithRoleForService(service, true, "admin2")
        );

        Logger logger = (Logger) LoggerFactory.getLogger(EventMessageHandler.class);
        logger.setLevel(Level.INFO);
        logger.addAppender(mockLogAppender);
    }

    @Test
    void shouldMarkMessageAsProcessed() throws Exception {
        disputeEvent = anEventFixture()
                .withEventType(EventType.DISPUTE_CREATED.name())
                .withEventDetails(objectMapper.valueToTree(Map.of("amount", 21000L, "evidence_due_date", "2022-03-07T13:00:00.001Z", "gateway_account_id", gatewayAccountId)))
                .withParentResourceExternalId("456")
                .build();
        when(mockServiceFinder.byGatewayAccountId(gatewayAccountId)).thenReturn(Optional.of(service));
        when(mockLedgerService.getTransaction(transaction.getTransactionId())).thenReturn(Optional.of(transaction));
        when(mockUserServices.getAdminUsersForService(service.getId())).thenReturn(users);

        var mockQueueMessage = mock(QueueMessage.class);
        var eventMessage = EventMessage.of(disputeEvent, mockQueueMessage);
        when(mockEventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));

        eventMessageHandler.processMessages();

        verify(mockEventSubscriberQueue).markMessageAsProcessed(mockQueueMessage);
    }

    @Test
    void shouldHandleDisputeCreatedEvent() throws QueueException {
        var mockQueueMessage = mock(QueueMessage.class);
        disputeEvent = anEventFixture()
                .withEventType(EventType.DISPUTE_CREATED.name())
                .withEventDetails(objectMapper.valueToTree(Map.of("amount", 21000L, "evidence_due_date", "2022-03-07T13:00:00.001Z", "gateway_account_id", gatewayAccountId, "reason", "fraudulent")))
                .withParentResourceExternalId("456")
                .build();
        var eventMessage = EventMessage.of(disputeEvent, mockQueueMessage);
        when(mockQueueMessage.getMessageId()).thenReturn("queue-message-id");
        when(mockEventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));

        when(mockServiceFinder.byGatewayAccountId(gatewayAccountId)).thenReturn(Optional.of(service));
        when(mockLedgerService.getTransaction(transaction.getTransactionId())).thenReturn(Optional.of(transaction));
        when(mockUserServices.getAdminUsersForService(service.getId())).thenReturn(users);

        eventMessageHandler.processMessages();

        verify(mockNotificationService, atMostOnce()).sendStripeDisputeCreatedEmail(adminEmailsCaptor.capture(), personalisationCaptor.capture());

        var emails = adminEmailsCaptor.getValue();
        var personalisation = personalisationCaptor.getValue();

        assertThat(emails.size(), is(2));
        assertThat(emails, hasItems("admin1@service.gov.uk", "admin2@service.gov.uk"));
        assertThat(personalisation.get("serviceName"), is(service.getName()));
        assertThat(personalisation.get("paymentExternalId"), is("456"));
        assertThat(personalisation.get("serviceReference"), is("tx ref"));
        assertThat(personalisation.get("paymentAmount"), is("210.00"));
        assertThat(personalisation.get("disputeEvidenceDueDate"), is("7 March 2022"));
        assertThat(personalisation.get("sendEvidenceToPayDueDate"), is("4 March 2022"));

        assertThat(personalisation.get("fraudulent"), is("yes"));
        assertThat(personalisation.get("disputedAmount"), is("210.00"));

        verify(mockLogAppender, times(2)).doAppend(loggingEventArgumentCaptor.capture());

        List<ILoggingEvent> logStatement = loggingEventArgumentCaptor.getAllValues();
        assertThat(logStatement.get(0).getFormattedMessage(), Is.is("Retrieved event queue message with id [queue-message-id] for resource external id [a-resource-external-id]"));
        assertThat(logStatement.get(1).getFormattedMessage(), Is.is("Processed notification email for disputed transaction"));
    }

    @Test
    void shouldHandleDisputeLostEvent_whenLiveAndEnableByDate() throws QueueException {
        var mockQueueMessage = mock(QueueMessage.class);
        disputeEvent = anEventFixture()
                .withEventType(EventType.DISPUTE_LOST.name())
                .withEventDetails(objectMapper.valueToTree(Map.of("net_amount", -4000L, "fee", 1500L, "amount", 2500L, "gateway_account_id", gatewayAccountId)))
                .withParentResourceExternalId("456")
                .withServiceId(service.getExternalId())
                .withLive(true)
                .build();
        var eventMessage = EventMessage.of(disputeEvent, mockQueueMessage);
        when(mockQueueMessage.getMessageId()).thenReturn("queue-message-id");
        when(mockEventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));
        when(mockNotificationService.getEmailNotificationsForLivePaymentsDisputeUpdatesFrom()).thenReturn(Instant.now().minusSeconds(6000L));
        when(mockServiceFinder.byGatewayAccountId(gatewayAccountId)).thenReturn(Optional.of(service));
        when(mockLedgerService.getTransaction(transaction.getTransactionId())).thenReturn(Optional.of(transaction));
        when(mockUserServices.getAdminUsersForService(service.getId())).thenReturn(users);

        eventMessageHandler.processMessages();

        verify(mockNotificationService, atMostOnce()).sendStripeDisputeLostEmail(adminEmailsCaptor.capture(), personalisationCaptor.capture());

        var emails = adminEmailsCaptor.getValue();
        var personalisation = personalisationCaptor.getValue();

        assertThat(emails.size(), is(2));
        assertThat(emails, hasItems("admin1@service.gov.uk", "admin2@service.gov.uk"));
        assertThat(personalisation.get("serviceName"), is(service.getName()));
        assertThat(personalisation.get("serviceReference"), is("tx ref"));
        assertThat(personalisation.get("organisationName"), is(service.getMerchantDetails().getName()));

        verify(mockLogAppender, times(2)).doAppend(loggingEventArgumentCaptor.capture());

        List<ILoggingEvent> logStatement = loggingEventArgumentCaptor.getAllValues();
        assertThat(logStatement.get(0).getFormattedMessage(), Is.is("Retrieved event queue message with id [queue-message-id] for resource external id [a-resource-external-id]"));
        assertThat(logStatement.get(1).getFormattedMessage(), Is.is("Processed notification email for disputed transaction"));
    }

    @Test
    void shouldHandleDisputeLostEvent_whenNotLiveAndEnabledByDate() throws QueueException {
        var mockQueueMessage = mock(QueueMessage.class);
        disputeEvent = anEventFixture()
                .withEventType(EventType.DISPUTE_LOST.name())
                .withEventDetails(objectMapper.valueToTree(Map.of("net_amount", -4000L,
                        "fee", 1500L,
                        "amount", 2500L,
                        "gateway_account_id", gatewayAccountId)))
                .withParentResourceExternalId("456")
                .withServiceId(service.getExternalId())
                .withLive(false)
                .build();
        var eventMessage = EventMessage.of(disputeEvent, mockQueueMessage);
        when(mockQueueMessage.getMessageId()).thenReturn("queue-message-id");
        when(mockEventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));
        when(mockNotificationService.getEmailNotificationsForTestPaymentsDisputeUpdatesFrom()).thenReturn(Instant.now().minusSeconds(6000L));
        when(mockServiceFinder.byGatewayAccountId(gatewayAccountId)).thenReturn(Optional.of(service));
        when(mockLedgerService.getTransaction(transaction.getTransactionId())).thenReturn(Optional.of(transaction));
        when(mockUserServices.getAdminUsersForService(service.getId())).thenReturn(users);

        eventMessageHandler.processMessages();

        verify(mockNotificationService, atMostOnce()).sendStripeDisputeLostEmail(adminEmailsCaptor.capture(), personalisationCaptor.capture());

        var emails = adminEmailsCaptor.getValue();
        var personalisation = personalisationCaptor.getValue();

        assertThat(emails.size(), is(2));
        assertThat(emails, hasItems("admin1@service.gov.uk", "admin2@service.gov.uk"));
        assertThat(personalisation.get("serviceName"), is(service.getName()));
        assertThat(personalisation.get("serviceReference"), is("tx ref"));
        assertThat(personalisation.get("organisationName"), is(service.getMerchantDetails().getName()));

        verify(mockLogAppender, times(2)).doAppend(loggingEventArgumentCaptor.capture());

        List<ILoggingEvent> logStatement = loggingEventArgumentCaptor.getAllValues();
        assertThat(logStatement.get(0).getFormattedMessage(), Is.is("Retrieved event queue message with id [queue-message-id] for resource external id [a-resource-external-id]"));
        assertThat(logStatement.get(1).getFormattedMessage(), Is.is("Processed notification email for disputed transaction"));
    }

    @Test
    void shouldNotCallNotificationService_whenNotLiveAndNotEnabledByDate() throws QueueException {
        var mockQueueMessage = mock(QueueMessage.class);
        disputeEvent = anEventFixture()
                .withEventType(EventType.DISPUTE_LOST.name())
                .withEventDetails(objectMapper.valueToTree(Map.of("net_amount", -4000L,
                        "fee", 1500L,
                        "amount", 2500L,
                        "gateway_account_id", gatewayAccountId)))
                .withParentResourceExternalId("456")
                .withServiceId(service.getExternalId())
                .withLive(false)
                .build();
        var eventMessage = EventMessage.of(disputeEvent, mockQueueMessage);
        when(mockQueueMessage.getMessageId()).thenReturn("queue-message-id");
        when(mockEventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));
        when(mockNotificationService.getEmailNotificationsForTestPaymentsDisputeUpdatesFrom()).thenReturn(Instant.now().plusSeconds(6000L));

        eventMessageHandler.processMessages();

        verify(mockNotificationService, never()).sendStripeDisputeLostEmail(anySet(), anyMap());

        verify(mockLogAppender, times(2)).doAppend(loggingEventArgumentCaptor.capture());

        List<ILoggingEvent> logStatement = loggingEventArgumentCaptor.getAllValues();
        assertThat(logStatement.get(0).getFormattedMessage(), Is.is("Retrieved event queue message with id [queue-message-id] for resource external id [a-resource-external-id]"));
        assertThat(logStatement.get(1).getFormattedMessage(), Is.is("Dispute lost email sending is not yet enabled for [service_id: " + service.getExternalId() + "]"));
    }

    @Test
    void shouldHandleDisputeWonEvent() throws QueueException {
        var mockQueueMessage = mock(QueueMessage.class);
        disputeEvent = anEventFixture()
                .withEventType(EventType.DISPUTE_WON.name())
                .withEventDetails(objectMapper.valueToTree(Map.of("gateway_account_id", gatewayAccountId)))
                .withParentResourceExternalId("456")
                .withServiceId(service.getExternalId())
                .withLive(true)
                .build();
        var eventMessage = EventMessage.of(disputeEvent, mockQueueMessage);
        when(mockQueueMessage.getMessageId()).thenReturn("queue-message-id");
        when(mockEventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));
        when(mockNotificationService.getEmailNotificationsForLivePaymentsDisputeUpdatesFrom()).thenReturn(Instant.now().minusSeconds(6000L));
        when(mockServiceFinder.byGatewayAccountId(gatewayAccountId)).thenReturn(Optional.of(service));
        when(mockLedgerService.getTransaction(transaction.getTransactionId())).thenReturn(Optional.of(transaction));
        when(mockUserServices.getAdminUsersForService(service.getId())).thenReturn(users);

        eventMessageHandler.processMessages();

        verify(mockNotificationService, atMostOnce()).sendStripeDisputeWonEmail(adminEmailsCaptor.capture(), personalisationCaptor.capture());

        var emails = adminEmailsCaptor.getValue();
        var personalisation = personalisationCaptor.getValue();

        assertThat(emails.size(), is(2));
        assertThat(emails, hasItems("admin1@service.gov.uk", "admin2@service.gov.uk"));
        assertThat(personalisation.get("serviceName"), is(service.getName()));
        assertThat(personalisation.get("serviceReference"), is("tx ref"));
        assertThat(personalisation.get("organisationName"), is(service.getMerchantDetails().getName()));

        verify(mockLogAppender, times(2)).doAppend(loggingEventArgumentCaptor.capture());

        List<ILoggingEvent> logStatement = loggingEventArgumentCaptor.getAllValues();
        assertThat(logStatement.get(0).getFormattedMessage(), Is.is("Retrieved event queue message with id [queue-message-id] for resource external id [a-resource-external-id]"));
        assertThat(logStatement.get(1).getFormattedMessage(), Is.is("Processed notification email for disputed transaction"));
    }

    @Test
    void shouldHandleDisputeEvidenceSubmittedEvent() throws QueueException {
        var mockQueueMessage = mock(QueueMessage.class);
        disputeEvent = anEventFixture()
                .withEventType(EventType.DISPUTE_EVIDENCE_SUBMITTED.name())
                .withEventDetails(objectMapper.valueToTree(Map.of("gateway_account_id", gatewayAccountId)))
                .withParentResourceExternalId("456")
                .withServiceId(service.getExternalId())
                .withLive(true)
                .build();
        var eventMessage = EventMessage.of(disputeEvent, mockQueueMessage);
        when(mockQueueMessage.getMessageId()).thenReturn("queue-message-id");
        when(mockEventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));
        when(mockNotificationService.getEmailNotificationsForLivePaymentsDisputeUpdatesFrom()).thenReturn(Instant.now().minusSeconds(6000L));
        when(mockServiceFinder.byGatewayAccountId(gatewayAccountId)).thenReturn(Optional.of(service));
        when(mockLedgerService.getTransaction(transaction.getTransactionId())).thenReturn(Optional.of(transaction));
        when(mockUserServices.getAdminUsersForService(service.getId())).thenReturn(users);

        eventMessageHandler.processMessages();

        verify(mockNotificationService, atMostOnce()).sendStripeDisputeEvidenceSubmittedEmail(adminEmailsCaptor.capture(), personalisationCaptor.capture());

        var emails = adminEmailsCaptor.getValue();
        var personalisation = personalisationCaptor.getValue();

        assertThat(emails.size(), is(2));
        assertThat(emails, hasItems("admin1@service.gov.uk", "admin2@service.gov.uk"));
        assertThat(personalisation.get("serviceName"), is(service.getName()));
        assertThat(personalisation.get("serviceReference"), is("tx ref"));
        assertThat(personalisation.get("organisationName"), is(service.getMerchantDetails().getName()));

        verify(mockLogAppender, times(2)).doAppend(loggingEventArgumentCaptor.capture());

        List<ILoggingEvent> logStatement = loggingEventArgumentCaptor.getAllValues();
        assertThat(logStatement.get(0).getFormattedMessage(), Is.is("Retrieved event queue message with id [queue-message-id] for resource external id [a-resource-external-id]"));
        assertThat(logStatement.get(1).getFormattedMessage(), Is.is("Processed notification email for disputed transaction"));
    }

    @Test
    void shouldNotCallNotificationService_whenLiveAndNotEnabledByDate() throws QueueException {
        var mockQueueMessage = mock(QueueMessage.class);
        disputeEvent = anEventFixture()
                .withEventType(EventType.DISPUTE_LOST.name())
                .withEventDetails(objectMapper.valueToTree(Map.of("net_amount", -4000L, "fee", 1500L, "amount", 2500L, "gateway_account_id", gatewayAccountId)))
                .withParentResourceExternalId("456")
                .withServiceId(service.getExternalId())
                .withLive(true)
                .build();
        var eventMessage = EventMessage.of(disputeEvent, mockQueueMessage);
        when(mockQueueMessage.getMessageId()).thenReturn("queue-message-id");
        when(mockEventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));
        when(mockNotificationService.getEmailNotificationsForLivePaymentsDisputeUpdatesFrom()).thenReturn(Instant.now().plusSeconds(6000L));

        eventMessageHandler.processMessages();

        verify(mockNotificationService, never()).sendStripeDisputeLostEmail(anySet(), anyMap());

        verify(mockLogAppender, times(2)).doAppend(loggingEventArgumentCaptor.capture());

        List<ILoggingEvent> logStatement = loggingEventArgumentCaptor.getAllValues();
        assertThat(logStatement.get(0).getFormattedMessage(), Is.is("Retrieved event queue message with id [queue-message-id] for resource external id [a-resource-external-id]"));
        assertThat(logStatement.get(1).getFormattedMessage(), Is.is("Dispute lost email sending is not yet enabled for [service_id: " + service.getExternalId() + "]"));
    }

    @Test
    void shouldNotCallNotificationServiceWhenServiceDoesNotExist() throws QueueException {
        var mockQueueMessage = mock(QueueMessage.class);
        disputeEvent = anEventFixture()
                .withEventType(EventType.DISPUTE_CREATED.name())
                .withEventDetails(objectMapper.valueToTree(Map.of("amount", 21000L, "fee", 1500L, "evidence_due_date", "2022-03-07T13:00:00Z", "gateway_account_id", gatewayAccountId)))
                .withParentResourceExternalId("456")
                .build();
        var eventMessage = EventMessage.of(disputeEvent, mockQueueMessage);
        when(mockEventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));
        when(mockServiceFinder.byGatewayAccountId(gatewayAccountId)).thenReturn(Optional.empty());

        eventMessageHandler.processMessages();

        verify(mockNotificationService, never()).sendStripeDisputeCreatedEmail(anySet(), anyMap());
    }

    @Test
    void shouldNotCallNotificationServiceWhenTransactionDoesNotExist() throws QueueException {
        var mockQueueMessage = mock(QueueMessage.class);
        disputeEvent = anEventFixture()
                .withEventType(EventType.DISPUTE_CREATED.name())
                .withEventDetails(objectMapper.valueToTree(Map.of("amount", 21000L, "fee", 1500L, "evidence_due_date", "2022-03-07T13:00:00.001Z", "gateway_account_id", gatewayAccountId)))
                .withParentResourceExternalId("456")
                .build();
        var eventMessage = EventMessage.of(disputeEvent, mockQueueMessage);
        when(mockEventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));
        when(mockServiceFinder.byGatewayAccountId(gatewayAccountId)).thenReturn(Optional.of(service));
        when(mockLedgerService.getTransaction(transaction.getTransactionId())).thenReturn(Optional.empty());

        eventMessageHandler.processMessages();

        verify(mockNotificationService, never()).sendStripeDisputeCreatedEmail(anySet(), anyMap());
    }

    @Test
    void shouldNotCallNotificationServiceWhenNoAdminUsersExist() throws QueueException {
        var mockQueueMessage = mock(QueueMessage.class);
        disputeEvent = anEventFixture()
                .withEventType(EventType.DISPUTE_CREATED.name())
                .withEventDetails(objectMapper.valueToTree(Map.of("amount", 21000L, "fee", 1500L, "evidence_due_date", "2022-03-07T13:00:00.001Z", "gateway_account_id", gatewayAccountId)))
                .withParentResourceExternalId("456")
                .build();
        var eventMessage = EventMessage.of(disputeEvent, mockQueueMessage);
        when(mockEventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));

        when(mockServiceFinder.byGatewayAccountId(gatewayAccountId)).thenReturn(Optional.of(service));
        when(mockLedgerService.getTransaction(transaction.getTransactionId())).thenReturn(Optional.of(transaction));
        when(mockUserServices.getAdminUsersForService(service.getId())).thenReturn(Collections.emptyList());

        eventMessageHandler.processMessages();

        verify(mockNotificationService, never()).sendStripeDisputeCreatedEmail(anySet(), anyMap());
    }
}
