package uk.gov.pay.adminusers.queue.event;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;
import uk.gov.pay.adminusers.client.ledger.service.LedgerService;
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
    private EventSubscriberQueue eventSubscriberQueue;

    @Mock
    private NotificationService mockNotificationService;

    @Mock
    private ServiceFinder mockServiceFinder;

    @Mock
    private UserServices mockUserServices;

    @Mock
    private LedgerService mockLedgerService;

    @Spy
    private ObjectMapper objectMapper;

    @Captor
    ArgumentCaptor<Set<String>> adminEmailsCaptor;

    @Captor
    ArgumentCaptor<Map<String, String>> personalisationCaptor;
    @Mock
    private Appender<ILoggingEvent> mockLogAppender;
    @Captor
    ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor;

    @InjectMocks
    private EventMessageHandler underTest;

    private Service serv;
    private LedgerTransaction tx;
    private List<UserEntity> users;
    private Event disputeEvent;

    @BeforeEach
    void setUp() {
        serv = Service.from(randomInt(), randomUuid(), new ServiceName(DEFAULT_NAME_VALUE));
        tx = aLedgerTransactionFixture()
                .withTransactionId("456")
                .withReference("tx ref")
                .build();
        users = Arrays.asList(
                aUserEntityWithRoleForService(serv, true, "admin1"),
                aUserEntityWithRoleForService(serv, true, "admin2")
        );
        disputeEvent = anEventFixture()
                .withEventType(EventType.DISPUTE_CREATED.name())
                .withEventDetails(new GsonBuilder().create().toJson(Map.of("amount", 21000L, "fee", 1500L, "evidence_due_date", 1646658000L)))
                .withServiceId(serv.getExternalId())
                .withParentResourceExternalId("456")
                .build();

        Logger logger = (Logger) LoggerFactory.getLogger(EventMessageHandler.class);
        logger.setLevel(Level.INFO);
        logger.addAppender(mockLogAppender);
    }

    @Test
    void shouldMarkMessageAsProcessed() throws Exception {
        Event event = anEventFixture().build();
        var mockQueueMessage = mock(QueueMessage.class);
        var eventMessage = EventMessage.of(event, mockQueueMessage);
        when(eventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));

        underTest.processMessages();

        verify(eventSubscriberQueue).markMessageAsProcessed(mockQueueMessage);
    }

    @Test
    void shouldHandleDisputeCreatedEvent() throws QueueException {

        var mockQueueMessage = mock(QueueMessage.class);
        var eventMessage = EventMessage.of(disputeEvent, mockQueueMessage);
        when(mockQueueMessage.getMessageId()).thenReturn("queue-message-id");
        when(eventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));

        when(mockServiceFinder.byExternalId(serv.getExternalId())).thenReturn(Optional.of(serv));
        when(mockLedgerService.getTransaction(tx.getTransactionId())).thenReturn(Optional.of(tx));
        when(mockUserServices.getAdminUsersForService(serv)).thenReturn(users);

        underTest.processMessages();

        verify(mockNotificationService, atMostOnce()).sendStripeDisputeCreatedEmail(adminEmailsCaptor.capture(), personalisationCaptor.capture());

        var emails = adminEmailsCaptor.getValue();
        var personalisation = personalisationCaptor.getValue();

        assertThat(emails.size(), is(2));
        assertThat(emails, hasItems("admin1@service.gov.uk", "admin2@service.gov.uk"));
        assertThat(personalisation.get("serviceName"), is(serv.getName()));
        assertThat(personalisation.get("paymentExternalId"), is("456"));
        assertThat(personalisation.get("serviceReference"), is("tx ref"));
        assertThat(personalisation.get("paymentAmount"), is("210.00"));
        assertThat(personalisation.get("disputeFee"), is("15.00"));
        assertThat(personalisation.get("disputeEvidenceDueDate"), is("7 March 2022"));
        assertThat(personalisation.get("sendEvidenceToPayDueDate"), is("4 March 2022"));

        verify(mockLogAppender, times(2)).doAppend(loggingEventArgumentCaptor.capture());

        List<ILoggingEvent> logStatement = loggingEventArgumentCaptor.getAllValues();
        assertThat(logStatement.get(0).getFormattedMessage(), Is.is("Retrieved event queue message with id [queue-message-id] for resource external id [a-resource-external-id]"));
        assertThat(logStatement.get(1).getFormattedMessage(), Is.is("Processed notification email for disputed transaction"));
    }

    @Test
    void shouldNotCallNotificationServiceWhenServiceDoesNotExist() throws QueueException {
        var mockQueueMessage = mock(QueueMessage.class);
        var eventMessage = EventMessage.of(disputeEvent, mockQueueMessage);
        when(eventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));
        when(mockServiceFinder.byExternalId(serv.getExternalId())).thenReturn(Optional.empty());

        underTest.processMessages();

        verify(mockNotificationService, never()).sendStripeDisputeCreatedEmail(anySet(), anyMap());
    }

    @Test
    void shouldNotCallNotificationServiceWhenTransactionDoesNotExist() throws QueueException {

        var mockQueueMessage = mock(QueueMessage.class);
        var eventMessage = EventMessage.of(disputeEvent, mockQueueMessage);
        when(eventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));
        when(mockServiceFinder.byExternalId(serv.getExternalId())).thenReturn(Optional.of(serv));
        when(mockLedgerService.getTransaction(tx.getTransactionId())).thenReturn(Optional.empty());

        underTest.processMessages();

        verify(mockNotificationService, never()).sendStripeDisputeCreatedEmail(anySet(), anyMap());
    }

    @Test
    void shouldNotCallNotificationServiceWhenNoAdminUsersExist() throws QueueException {
        var mockQueueMessage = mock(QueueMessage.class);
        var eventMessage = EventMessage.of(disputeEvent, mockQueueMessage);
        when(eventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));

        when(mockServiceFinder.byExternalId(serv.getExternalId())).thenReturn(Optional.of(serv));
        when(mockLedgerService.getTransaction(tx.getTransactionId())).thenReturn(Optional.of(tx));
        when(mockUserServices.getAdminUsersForService(serv)).thenReturn(Collections.emptyList());

        underTest.processMessages();

        verify(mockNotificationService, never()).sendStripeDisputeCreatedEmail(anySet(), anyMap());
    }
}
