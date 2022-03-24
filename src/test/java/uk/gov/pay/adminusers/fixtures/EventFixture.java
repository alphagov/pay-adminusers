package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.queue.model.Event;

public class EventFixture {
    
    private String resourceExternalId = "a-resource-external-id";
    private String parentResourceExternalId;
    private String eventType = "AN_EVENT_TYPE";
    private String eventDetails;

    private EventFixture() {
    }
    
    public static EventFixture anEventFixture() {
        return new EventFixture();
    }
    
    public EventFixture withResourceExternalId(String resourceExternalId) {
        this.resourceExternalId = resourceExternalId;
        return this;
    }
    
    public EventFixture withParentResourceExternalId(String parentResourceExternalId) {
        this.parentResourceExternalId = parentResourceExternalId;
        return this;
    }
    
    public EventFixture withEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }
    
    public EventFixture withEventDetails(String eventDetails) {
        this.eventDetails = eventDetails;
        return this;
    }
    
    public Event build() {
        return new Event(
                resourceExternalId,
                parentResourceExternalId,
                eventType,
                eventDetails);
    }
}
