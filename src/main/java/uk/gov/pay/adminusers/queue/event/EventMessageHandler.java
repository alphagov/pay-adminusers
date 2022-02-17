package uk.gov.pay.adminusers.queue.event;

import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.queue.model.EventMessage;
import uk.gov.service.payments.commons.queue.exception.QueueException;

import javax.inject.Inject;
import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.kv;

public class EventMessageHandler {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final EventSubscriberQueue eventSubscriberQueue;

    @Inject
    public EventMessageHandler(EventSubscriberQueue eventSubscriberQueue) {
        this.eventSubscriberQueue = eventSubscriberQueue;
    }
    
    public void processMessages() throws QueueException {
        List<EventMessage> eventMessages = eventSubscriberQueue.retrieveEvents();
        for (EventMessage message : eventMessages) {
            try {
                logger.info("Retrieved event queue message with id {} for resource external id {}",
                        message.getQueueMessage().getMessageId(), message.getEvent().getResourceExternalId());
                
                eventSubscriberQueue.markMessageAsProcessed(message.getQueueMessage());
            } catch (Exception e) {
                Sentry.captureException(e);
                logger.warn("Error during handling the event message",
                        kv("sqs_message_id", message.getQueueMessage().getMessageId()),
                        kv("resource_external_id", message.getEvent().getResourceExternalId()),
                        kv("error", e.getMessage())
                );
            }
        }
    }
}
