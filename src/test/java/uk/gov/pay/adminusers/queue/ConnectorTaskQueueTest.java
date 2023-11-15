package uk.gov.pay.adminusers.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.app.config.ConnectorTaskQueueConfig;
import uk.gov.pay.adminusers.app.config.SqsConfig;
import uk.gov.pay.adminusers.queue.model.ConnectorTask;
import uk.gov.pay.adminusers.queue.model.ServiceArchivedTaskData;
import uk.gov.service.payments.commons.queue.model.QueueMessage;
import uk.gov.service.payments.commons.queue.sqs.SqsQueueService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConnectorTaskQueueTest {

    @Mock
    SqsQueueService sqsQueueService;
    
    @Mock
    AdminUsersConfig adminUsersConfig;

    ObjectMapper objectMapper = new ObjectMapper();
    
    @Mock
    SqsConfig sqsConfig;
    
    @Mock
    private ConnectorTaskQueueConfig connectorTaskQueueConfig;

    @BeforeEach
    void setup() {
        when(adminUsersConfig.getSqsConfig()).thenReturn(sqsConfig);
        when(sqsConfig.getConnectorTasksQueueUrl()).thenReturn("http://connector-task-queue-url");
        when(adminUsersConfig.getConnectorTaskQueueConfig()).thenReturn(connectorTaskQueueConfig);
        when(connectorTaskQueueConfig.getFailedMessageRetryDelayInSeconds()).thenReturn(0);
    }
    
    @Test
    void shouldSendValidSerialisedServiceArchivedMessageToQueue() throws Exception {
        when(sqsQueueService.sendMessage(anyString(), anyString())).thenReturn(mock(QueueMessage.class));
        
        ConnectorTaskQueue connectorTaskQueue = new ConnectorTaskQueue(sqsQueueService, adminUsersConfig, objectMapper);
        connectorTaskQueue.addTaskToQueue(new ConnectorTask(new ServiceArchivedTaskData("serviceId"), "service_archived"));

        verify(sqsQueueService).sendMessage(adminUsersConfig.getSqsConfig().getConnectorTasksQueueUrl(),
                "{\"data\":\"{\\\"service_external_id\\\":\\\"serviceId\\\"}\",\"task\":\"service_archived\"}");
    }
}
