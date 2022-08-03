package uk.gov.pay.adminusers.queue.model.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.queue.model.Event;
import uk.gov.pay.adminusers.queue.model.EventType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.adminusers.TestTemplateResourceLoader.DISPUTE_LOST_EVENT;
import static uk.gov.pay.adminusers.TestTemplateResourceLoader.load;

class DisputeLostDetailsTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper(); 

    @Test
    public void shouldDeserialiseDisputeEvent() throws JsonProcessingException {

        var json = objectMapper.readTree(load(DISPUTE_LOST_EVENT));
        var evt = objectMapper.treeToValue(json, Event.class);
        var disputeLostDetails = objectMapper.treeToValue(evt.getEventDetails(), DisputeLostDetails.class);
        
        assertThat(evt.getEventType(), is(EventType.DISPUTE_LOST.name()));
        assertThat(disputeLostDetails.getFee(), is(1500L));
        assertThat(disputeLostDetails.getAmount(), is(2500L));
        assertThat(disputeLostDetails.getNetAmount(), is(-4000L));
        assertThat(disputeLostDetails.getGatewayAccountId(), is("123"));
    }
}
