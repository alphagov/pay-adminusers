package uk.gov.pay.adminusers.queue.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.queue.model.Event;
import uk.gov.pay.adminusers.queue.model.EventMessage;
import uk.gov.pay.adminusers.queue.model.SNSMessage;
import uk.gov.service.payments.commons.queue.exception.QueueException;
import uk.gov.service.payments.commons.queue.model.QueueMessage;
import uk.gov.service.payments.commons.queue.sqs.AbstractQueue;
import uk.gov.service.payments.commons.queue.sqs.SqsQueueService;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EventSubscriberQueue extends AbstractQueue {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    public EventSubscriberQueue(SqsQueueService sqsQueueService, AdminUsersConfig adminUsersConfig, ObjectMapper objectMapper) {
        super(sqsQueueService,
                objectMapper,
                adminUsersConfig.getSqsConfig().getEventSubscriberQueueUrl(),
                adminUsersConfig.getEventSubscriberQueueConfig().getFailedMessageRetryDelayInSeconds());
    }
    
    public List<EventMessage> retrieveEvents() throws QueueException {
        List<QueueMessage> queueMessages = retrieveMessages();

        return queueMessages
                .stream()
                .map(this::deserializeMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private EventMessage deserializeMessage(QueueMessage queueMessage) {
        try {
            SNSMessage snsMessage = objectMapper.readValue(queueMessage.getMessageBody(), SNSMessage.class);
            Event event = objectMapper.readValue(snsMessage.getMessage(), Event.class);
            
            return EventMessage.of(event, queueMessage);
        } catch (IOException e) {
            logger.warn(
                    "There was an exception parsing message [messageId={}] into an [{}]",
                    queueMessage.getMessageId(),
                    EventMessage.class);

            return null;
        }
    }
}
