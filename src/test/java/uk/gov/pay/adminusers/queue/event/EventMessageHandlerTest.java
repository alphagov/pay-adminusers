package uk.gov.pay.adminusers.queue.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.queue.model.Event;
import uk.gov.pay.adminusers.queue.model.EventMessage;
import uk.gov.service.payments.commons.queue.model.QueueMessage;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.fixtures.EventFixture.anEventFixture;

@ExtendWith(MockitoExtension.class)
class EventMessageHandlerTest {

    @Mock
    private EventSubscriberQueue eventSubscriberQueue;
    
    @InjectMocks
    private EventMessageHandler eventMessageHandler;

    @BeforeEach
    void setUp() {
    }

    @Test
    void shouldMarkMessageAsProcessed() throws Exception {
        Event event = anEventFixture().build();
        var mockQueueMessage = mock(QueueMessage.class);
        var eventMessage = EventMessage.of(event, mockQueueMessage);
        when(eventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));
                
        eventMessageHandler.processMessages();
        
        verify(eventSubscriberQueue).markMessageAsProcessed(mockQueueMessage);
    }
}
