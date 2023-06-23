package uk.gov.pay.adminusers.queue.model.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.queue.model.Event;
import uk.gov.pay.adminusers.queue.model.EventType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.adminusers.JsonResourceLoader.DISPUTE_WON_EVENT;
import static uk.gov.pay.adminusers.JsonResourceLoader.load;

class DisputeWonDetailsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void shouldDeserialiseDisputeEvent() throws Exception {

        var json = objectMapper.readTree(load(DISPUTE_WON_EVENT));
        var evt = objectMapper.treeToValue(json, Event.class);
        var disputeLostDetails = objectMapper.treeToValue(evt.getEventDetails(), DisputeWonDetails.class);

        assertThat(evt.getEventType(), is(EventType.DISPUTE_WON.name()));
        assertThat(disputeLostDetails.getGatewayAccountId(), is("123"));
    }
}
