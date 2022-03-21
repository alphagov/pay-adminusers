package uk.gov.pay.adminusers.queue.event;

import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
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
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        Map<String, String> eventDetails = Map.of("example_event_details_field", "and its value");
        String serviceId = "a-service-id";
        String resourceExternalId = "resource-id";
        String parentResourceExternalId = "parent-id";
        String eventType = "PAYMENT_CREATED";

        String messageBody = new GsonBuilder().create().toJson(
                Map.of(
                        "service_id", serviceId,
                        "resource_external_id", resourceExternalId,
                        "parent_resource_external_id", parentResourceExternalId,
                        "event_type", eventType,
                        "event_details", eventDetails,
                        "ignored_field", "to check we are ignoring fields we don't care about"
                ));
        String validJsonMessage = new GsonBuilder().create()
                .toJson(Map.of("Message", messageBody));

        var sendMessageResult = mock(SendMessageResult.class);
        List<QueueMessage> messages = List.of(
                QueueMessage.of(sendMessageResult, validJsonMessage)
        );
        when(sqsQueueService.receiveMessages(anyString(), anyString())).thenReturn(messages);

        List<EventMessage> eventMessages = eventSubscriberQueue.retrieveEvents();
        assertThat(eventMessages, hasSize(1));
        Event event = eventMessages.get(0).getEvent();
        assertThat(event.getServiceId(), is(serviceId));
        assertThat(event.getResourceExternalId(), is(resourceExternalId));
        assertThat(event.getParentResourceExternalId(), is(parentResourceExternalId));
        assertThat(event.getEventType(), is(eventType));

        String expectedEventDetailsString = new GsonBuilder().create().toJson(eventDetails);
        JsonNode expectedEventDetails = objectMapper.readTree(expectedEventDetailsString);
        assertThat(event.getEventData(), is(expectedEventDetails));
    }
}
