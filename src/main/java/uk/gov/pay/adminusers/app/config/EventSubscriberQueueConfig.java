package uk.gov.pay.adminusers.app.config;

public class EventSubscriberQueueConfig {

    private Boolean eventSubscriberQueueEnabled;
    private int queueSchedulerNumberOfThreads;
    private int queueSchedulerThreadDelayInSeconds;
    private int failedMessageRetryDelayInSeconds;
    private int queueSchedulerShutdownTimeoutInSeconds;

    public Boolean getEventSubscriberQueueEnabled() {
        return eventSubscriberQueueEnabled;
    }

    public int getQueueSchedulerNumberOfThreads() {
        return queueSchedulerNumberOfThreads;
    }

    public int getQueueSchedulerThreadDelayInSeconds() {
        return queueSchedulerThreadDelayInSeconds;
    }

    public int getFailedMessageRetryDelayInSeconds() {
        return failedMessageRetryDelayInSeconds;
    }

    public int getQueueSchedulerShutdownTimeoutInSeconds() {
        return queueSchedulerShutdownTimeoutInSeconds;
    }
}
