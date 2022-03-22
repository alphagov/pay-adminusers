package uk.gov.pay.adminusers.queue.event;

import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.app.config.EventSubscriberQueueConfig;
import uk.gov.pay.adminusers.app.config.SqsConfig;
import uk.gov.pay.adminusers.queue.model.Event;
import uk.gov.pay.adminusers.queue.model.EventMessage;
import uk.gov.service.payments.commons.queue.model.QueueMessage;
import uk.gov.service.payments.commons.queue.sqs.SqsQueueService;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.TestTemplateResourceLoader.DISPUTE_CREATED_SNS_MESSAGE;
import static uk.gov.pay.adminusers.TestTemplateResourceLoader.load;

@ExtendWith(MockitoExtension.class)
class EventSubscriberQueueTest {

    @Mock
    private AdminUsersConfig adminUsersConfig;

    @Mock
    private SqsConfig sqsConfig;

    @Mock
    private EventSubscriberQueueConfig eventSubscriberQueueConfig;

    @Mock
    private SqsQueueService sqsQueueService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EventSubscriberQueue eventSubscriberQueue;

    @BeforeEach
    void setUp() {
        when(sqsConfig.getEventSubscriberQueueUrl()).thenReturn("");
        when(eventSubscriberQueueConfig.getFailedMessageRetryDelayInSeconds()).thenReturn(900);
        when(adminUsersConfig.getSqsConfig()).thenReturn(sqsConfig);
        when(adminUsersConfig.getEventSubscriberQueueConfig()).thenReturn(eventSubscriberQueueConfig);

        eventSubscriberQueue = new EventSubscriberQueue(sqsQueueService, adminUsersConfig, objectMapper);
    }

    @Test
    void shouldRetrieveEventsForCorrectlyFormattedJSON() throws Exception {
        String message = load(DISPUTE_CREATED_SNS_MESSAGE);

        var sendMessageResult = mock(SendMessageResult.class);
        List<QueueMessage> messages = List.of(
                QueueMessage.of(sendMessageResult, message)
        );
        when(sqsQueueService.receiveMessages(anyString(), anyString())).thenReturn(messages);

        List<EventMessage> eventMessages = eventSubscriberQueue.retrieveEvents();
        assertThat(eventMessages, hasSize(1));
        Event event = eventMessages.get(0).getEvent();
        assertThat(event.getServiceId(), is("5e0207ee342048d4ac4d1d05dd9ek3js"));
        assertThat(event.getResourceExternalId(), is("dp_1KfoljHj08j2jFuBkNEd89sd"));
        assertThat(event.getParentResourceExternalId(), is("pk8vak8vfiii5hjvqpsa4dsd"));
        assertThat(event.getEventType(), is("DISPUTE_CREATED"));
        
        assertThat(event.getEventDetails(), is("{\"fee\":1500,\"evidence_due_date\":1648684799,\"gateway_account_id\":\"528\",\"amount\":1000,\"net_amount\":2500,\"reason\":\"fraudulent\"}"));
    }
}
