package uk.gov.pay.adminusers.queue.model.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.queue.model.Event;
import uk.gov.pay.adminusers.queue.model.EventType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.adminusers.TestTemplateResourceLoader.DISPUTE_CREATED_EVENT;
import static uk.gov.pay.adminusers.TestTemplateResourceLoader.load;

class DisputeCreatedDetailsTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper(); 

    @Test
    public void shouldDeserialiseDisputeEvent() throws JsonProcessingException {

        var json = objectMapper.readTree(load(DISPUTE_CREATED_EVENT));
        var evt = objectMapper.treeToValue(json, Event.class);
        var disputeCreatedDetails = objectMapper.treeToValue(evt.getEventData(), DisputeCreatedDetails.class);
        
        assertThat(evt.getEventType(), is(EventType.DISPUTE_CREATED.name()));
        assertThat(disputeCreatedDetails.getDisputeFee(), is(1500L));
        assertThat(disputeCreatedDetails.getPaymentAmount(), is(125000L));
        assertThat(disputeCreatedDetails.getDisputeEvidenceDueDate(), is(1648745127L));
    }
}
