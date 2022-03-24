package uk.gov.pay.adminusers.queue.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;
import uk.gov.pay.adminusers.client.ledger.service.LedgerService;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.queue.model.Event;
import uk.gov.pay.adminusers.queue.model.EventMessage;
import uk.gov.pay.adminusers.queue.model.EventType;
import uk.gov.pay.adminusers.queue.model.event.DisputeCreatedDetails;
import uk.gov.pay.adminusers.service.NotificationService;
import uk.gov.pay.adminusers.service.ServiceFinder;
import uk.gov.pay.adminusers.service.UserServices;
import uk.gov.service.payments.commons.queue.exception.QueueException;

import javax.inject.Inject;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.pay.adminusers.utils.currency.ConvertToCurrency.convertPenceToPounds;
import static uk.gov.pay.adminusers.utils.date.DisputeEvidenceDueByDateUtil.getPayDueByDateForEpoch;
import static uk.gov.pay.adminusers.utils.date.DisputeEvidenceDueByDateUtil.getZDTForEpoch;
import static uk.gov.service.payments.logging.LoggingKeys.GATEWAY_DISPUTE_ID;
import static uk.gov.service.payments.logging.LoggingKeys.PAYMENT_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.SERVICE_EXTERNAL_ID;

public class EventMessageHandler {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy"); // 9 March 2022
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final EventSubscriberQueue eventSubscriberQueue;
    private final LedgerService ledgerService;
    private final NotificationService notificationService;
    private final ServiceFinder serviceFinder;
    private final UserServices userServices;
    private final ObjectMapper objectMapper;

    @Inject
    public EventMessageHandler(EventSubscriberQueue eventSubscriberQueue, 
                               LedgerService ledgerService, 
                               NotificationService notificationService, 
                               ServiceFinder serviceFinder, 
                               UserServices userServices, 
                               ObjectMapper objectMapper) {
        this.eventSubscriberQueue = eventSubscriberQueue;
        this.ledgerService = ledgerService;
        this.notificationService = notificationService;
        this.serviceFinder = serviceFinder;
        this.userServices = userServices;
        this.objectMapper = objectMapper;
    }

    public void processMessages() throws QueueException {
        List<EventMessage> eventMessages = eventSubscriberQueue.retrieveEvents();
        for (EventMessage message : eventMessages) {
            try {
                logger.info("Retrieved event queue message with id [{}] for resource external id [{}]",
                        message.getQueueMessage().getMessageId(), message.getEvent().getResourceExternalId());
                if (message.getEvent().getEventType().equalsIgnoreCase(EventType.DISPUTE_CREATED.name())) {
                    handleDisputeCreatedMessage(message.getEvent());
                } else {
                    logger.warn("Unknown event type: {}", message.getEvent().getEventType());
                }
                eventSubscriberQueue.markMessageAsProcessed(message.getQueueMessage());
            } catch (Exception e) {
                Sentry.captureException(e);
                logger.warn("An error occurred handling the event message",
                        kv("sqs_message_id", message.getQueueMessage().getMessageId()),
                        kv("resource_external_id", message.getEvent().getResourceExternalId()),
                        kv("error", e.getMessage())
                );
            }
        }
    }

    private void handleDisputeCreatedMessage(Event disputeCreatedEvent) throws JsonProcessingException {
        MDC.put(GATEWAY_DISPUTE_ID, disputeCreatedEvent.getResourceExternalId());
        MDC.put(PAYMENT_EXTERNAL_ID, disputeCreatedEvent.getParentResourceExternalId());
        MDC.put(SERVICE_EXTERNAL_ID, disputeCreatedEvent.getServiceId());

        var disputeCreatedDetails = objectMapper.readValue(disputeCreatedEvent.getEventDetails(), DisputeCreatedDetails.class);

        Service service = serviceFinder.byExternalId(disputeCreatedEvent.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException(format("Service not found [external_id: %s]", disputeCreatedEvent.getServiceId())));
        LedgerTransaction transaction = ledgerService.getTransaction(disputeCreatedEvent.getParentResourceExternalId())
                .orElseThrow(() -> new IllegalArgumentException(format("Transaction not found [payment_external_id: %s]", disputeCreatedEvent.getParentResourceExternalId())));
        
        List<UserEntity> serviceAdmins = userServices.getAdminUsersForService(service);

        var epoch = disputeCreatedDetails.getEvidenceDueDate();
        String formattedDueDate = getZDTForEpoch(epoch).format(DATE_TIME_FORMATTER);
        String formattedPayDueDate = getPayDueByDateForEpoch(epoch).format(DATE_TIME_FORMATTER);

        var paymentAmountInPounds = convertPenceToPounds.apply(disputeCreatedDetails.getAmount()).toString();
        var disputeFeeInPounds = convertPenceToPounds.apply(disputeCreatedDetails.getFee()).toString();

        Map<String, String> personalisation = Stream.of(new String[][]{
                {"serviceName", service.getName()},
                {"paymentExternalId", disputeCreatedEvent.getParentResourceExternalId()},
                {"serviceReference", transaction.getReference()},
                {"paymentAmount", paymentAmountInPounds},
                {"disputeFee", disputeFeeInPounds},
                {"disputeEvidenceDueDate", formattedDueDate},
                {"sendEvidenceToPayDueDate", formattedPayDueDate}
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

        if (!serviceAdmins.isEmpty()){
            notificationService.sendStripeDisputeCreatedEmail(
                    serviceAdmins.stream().map(UserEntity::getEmail).collect(Collectors.toSet()),
                    personalisation
            );
            logger.info("Processed notification email for disputed transaction");
        } else {
            throw new IllegalStateException(format("Service has no Admin users [external_id: %s]", service.getExternalId()));
        }

        List.of(PAYMENT_EXTERNAL_ID, GATEWAY_DISPUTE_ID, SERVICE_EXTERNAL_ID).forEach(MDC::remove);
    }
}
