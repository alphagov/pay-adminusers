package uk.gov.pay.adminusers.queue.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Event {
    
    private String serviceId;
    private String resourceExternalId;
    private String parentResourceExternalId;
    private String eventType;
    private String eventDetails;
    
    public Event() {
        // for deserialization
    }
    
    public Event(String serviceId,
                 String resourceExternalId,
                 String parentResourceExternalId,
                 String eventType,
                 String eventDetails) {
        this.serviceId = serviceId;
        this.resourceExternalId = resourceExternalId;
        this.parentResourceExternalId = parentResourceExternalId;
        this.eventType = eventType;
        this.eventDetails = eventDetails;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getResourceExternalId() {
        return resourceExternalId;
    }

    public String getParentResourceExternalId() {
        return parentResourceExternalId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventDetails() {
        return eventDetails;
    }

    @Override
    public String toString() {
        return "Event{" +
                "serviceId='" + serviceId + '\'' +
                ", resourceExternalId='" + resourceExternalId + '\'' +
                ", parentResourceExternalId='" + parentResourceExternalId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", eventDetails=" + eventDetails +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(serviceId, event.serviceId) && Objects.equals(resourceExternalId, event.resourceExternalId) && Objects.equals(parentResourceExternalId, event.parentResourceExternalId) && Objects.equals(eventType, event.eventType) && Objects.equals(eventDetails, event.eventDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, resourceExternalId, parentResourceExternalId, eventType, eventDetails);
    }
}
