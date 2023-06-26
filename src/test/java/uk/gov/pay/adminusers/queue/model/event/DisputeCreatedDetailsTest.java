package uk.gov.pay.adminusers.queue.model.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.queue.model.Event;
import uk.gov.pay.adminusers.queue.model.EventType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.adminusers.JsonResourceLoader.DISPUTE_CREATED_EVENT;
import static uk.gov.pay.adminusers.JsonResourceLoader.load;

class DisputeCreatedDetailsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void shouldDeserialiseDisputeEvent() throws Exception {

        var json = objectMapper.readTree(load(DISPUTE_CREATED_EVENT));
        var evt = objectMapper.treeToValue(json, Event.class);
        var disputeCreatedDetails = objectMapper.treeToValue(evt.getEventDetails(), DisputeCreatedDetails.class);

        assertThat(evt.getEventType(), is(EventType.DISPUTE_CREATED.name()));
        assertThat(disputeCreatedDetails.getAmount(), is(125000L));
        assertThat(disputeCreatedDetails.getEvidenceDueDate().toString(), is("2022-03-07T13:00:00.001Z"));
        assertThat(disputeCreatedDetails.getGatewayAccountId(), is("123"));
        assertThat(disputeCreatedDetails.getReason(), is("fraudulent"));
    }
}
