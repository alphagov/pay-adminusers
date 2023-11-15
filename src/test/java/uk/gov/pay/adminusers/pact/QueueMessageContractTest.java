package uk.gov.pay.adminusers.pact;

import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit.target.AmqpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.pay.adminusers.queue.model.ConnectorTask;
import uk.gov.pay.adminusers.queue.model.ServiceArchivedTaskData;

public class QueueMessageContractTest {

    @TestTarget
    public final Target target = new AmqpTarget();
    
    private ObjectMapper objectMapper = new ObjectMapper();

    @PactVerifyProvider("a service archived event")
    public String verifyServiceArchivedEvent() throws JsonProcessingException {
        ConnectorTask event =
                new ConnectorTask(new ServiceArchivedTaskData("service-external-id"), "service_archived");
        return objectMapper.writeValueAsString(event);
    }
}
