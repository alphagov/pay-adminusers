package uk.gov.pay.adminusers.app.config;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

public class SqsConfig {

    @NotNull
    private boolean nonStandardServiceEndpoint;
    private String endpoint;
    @NotNull
    private String region;
    private String accessKey;
    private String secretKey;
    
    @NotNull
    private String eventSubscriberQueueUrl;
    
    @NotNull
    private String connectorTasksQueueUrl;

    @Max(20)
    private int messageMaximumWaitTimeInSeconds;
    @Max(10)
    private int messageMaximumBatchSize;

    public String getEndpoint() {
        return endpoint;
    }

    public String getRegion() {
        return region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public boolean isNonStandardServiceEndpoint() {
        return nonStandardServiceEndpoint;
    }
    
    public String getEventSubscriberQueueUrl() {
        return eventSubscriberQueueUrl;
    }

    public int getMessageMaximumWaitTimeInSeconds() {
        return messageMaximumWaitTimeInSeconds;
    }

    public int getMessageMaximumBatchSize() {
        return messageMaximumBatchSize;
    }

    public String getConnectorTasksQueueUrl() {
        return connectorTasksQueueUrl;
    }
}
