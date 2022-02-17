package uk.gov.pay.adminusers.queue.model;

import uk.gov.service.payments.commons.queue.model.QueueMessage;

public class EventMessage {
    private Event event;
    private QueueMessage queueMessage;

    public EventMessage(Event event, QueueMessage queueMessage) {
        this.event = event;
        this.queueMessage = queueMessage;
    }

    public static EventMessage of(Event event, QueueMessage queueMessage) {
        return new EventMessage(event, queueMessage);
    }

    public QueueMessage getQueueMessage() {
        return queueMessage;
    }

    public Event getEvent() {
        return event;
    }
}
