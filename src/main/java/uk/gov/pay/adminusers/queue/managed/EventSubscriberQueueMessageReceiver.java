package uk.gov.pay.adminusers.queue.managed;

import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.app.config.EventSubscriberQueueConfig;
import uk.gov.pay.adminusers.queue.event.EventMessageHandler;

import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventSubscriberQueueMessageReceiver implements Managed {
    
    private static final String THREAD_NAME = "sqs-message-eventSubscriberQueueMessageReceiver";
    private static final int SCHEDULER_NUMBER_OF_THREADS = 1;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final EventMessageHandler eventMessageHandler;
    private final ScheduledExecutorService scheduledExecutorService;

    private final int queueSchedulerThreadDelayInSeconds;
    private final int queueSchedulerShutdownTimeoutInSeconds;
    private boolean queueEnabled;

    @Inject
    public EventSubscriberQueueMessageReceiver(EventMessageHandler eventMessageHandler, Environment environment,
                                               AdminUsersConfig adminUsersConfig) {
        this.eventMessageHandler = eventMessageHandler;
        
        scheduledExecutorService = environment
                .lifecycle()
                .scheduledExecutorService(THREAD_NAME)
                .threads(SCHEDULER_NUMBER_OF_THREADS)
                .build();

        EventSubscriberQueueConfig eventSubscriberQueueConfig = adminUsersConfig.getEventSubscriberQueueConfig();
        queueSchedulerThreadDelayInSeconds = eventSubscriberQueueConfig.getQueueSchedulerThreadDelayInSeconds();
        queueSchedulerShutdownTimeoutInSeconds = eventSubscriberQueueConfig.getQueueSchedulerShutdownTimeoutInSeconds();
    }

    @Override
    public void start() {
        if (queueEnabled) {
            int initialDelay = queueSchedulerThreadDelayInSeconds;
            scheduledExecutorService.scheduleWithFixedDelay(
                    this::processMessages,
                    initialDelay,
                    queueSchedulerThreadDelayInSeconds,
                    TimeUnit.SECONDS);
        }
    }

    private void processMessages() {
        logger.info("Queue message receiver thread polling queue");
        try {
            eventMessageHandler.processMessages();
        } catch (Exception e) {
            logger.error("Queue message receiver thread exception", e);
        }
    }

    @Override
    public void stop() {
        logger.info("Shutting down event subscriber queue message receiver");
        scheduledExecutorService.shutdown();
        try {
            if (scheduledExecutorService.awaitTermination(queueSchedulerShutdownTimeoutInSeconds, TimeUnit.SECONDS)) {
                logger.info("Event subscriber queue message receiver shut down cleanly");
            } else {
                logger.error("Event subscriber queue still processing messages after shutdown wait time will now be forcefully stopped");
                scheduledExecutorService.shutdownNow();
                if (!scheduledExecutorService.awaitTermination(12, TimeUnit.SECONDS)) {
                    logger.error("Event subscriber queue receiver could not be forced stopped");
                }
            }
        } catch (InterruptedException ex) {
            logger.error("Failed to shutdown event subscriber queue message receiver cleanly as the wait was interrupted.");
            scheduledExecutorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
