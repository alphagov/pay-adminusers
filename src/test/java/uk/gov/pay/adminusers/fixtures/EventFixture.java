package uk.gov.pay.adminusers.fixtures;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.pay.adminusers.queue.model.Event;

public class EventFixture {
    
    private String serviceId = "a-service-id";
    private String resourceExternalId = "a-resource-external-id";
    private String parentResourceExternalId;
    private String eventType = "AN_EVENT_TYPE";
    private JsonNode eventData;

    private EventFixture() {
    }
    
    public static EventFixture anEventFixture() {
        return new EventFixture();
    }
    
    public EventFixture withServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
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
    
    public EventFixture withEventData(JsonNode eventData) {
        this.eventData = eventData;
        return this;
    }
    
    public Event build() {
        return new Event(
                serviceId,
                resourceExternalId,
                parentResourceExternalId,
                eventType,
                eventData);
    }
}
