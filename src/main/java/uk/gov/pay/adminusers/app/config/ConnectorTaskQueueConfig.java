package uk.gov.pay.adminusers.app.config;

public class ConnectorTaskQueueConfig {

    private int failedMessageRetryDelayInSeconds;
    
    public int getFailedMessageRetryDelayInSeconds() {
        return failedMessageRetryDelayInSeconds;
    }
}
