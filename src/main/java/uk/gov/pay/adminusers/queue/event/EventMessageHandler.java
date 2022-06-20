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
import uk.gov.pay.adminusers.queue.model.event.DisputeLostDetails;
import uk.gov.pay.adminusers.queue.model.event.DisputeWonDetails;
import uk.gov.pay.adminusers.service.NotificationService;
import uk.gov.pay.adminusers.service.ServiceFinder;
import uk.gov.pay.adminusers.service.UserServices;
import uk.gov.service.payments.commons.queue.exception.QueueException;

import javax.inject.Inject;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.pay.adminusers.utils.currency.ConvertToCurrency.convertPenceToPounds;
import static uk.gov.pay.adminusers.utils.date.DisputeEvidenceDueByDateUtil.getPayDueByDateForEpoch;
import static uk.gov.pay.adminusers.utils.date.DisputeEvidenceDueByDateUtil.getZDTForEpoch;
import static uk.gov.service.payments.logging.LoggingKeys.GATEWAY_ACCOUNT_ID;
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
                } else if (message.getEvent().getEventType().equalsIgnoreCase(EventType.DISPUTE_LOST.name())) {
                    handleDisputeLostMessage(message.getEvent());
                } else if (message.getEvent().getEventType().equalsIgnoreCase(EventType.DISPUTE_WON.name())) {
                    handleDisputeWonMessage(message.getEvent());
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

    private void handleDisputeWonMessage(Event disputeWonEvent) throws JsonProcessingException {
        try {
            setupMDC(disputeWonEvent);

            var disputeWonDetails = objectMapper.readValue(disputeWonEvent.getEventDetails(), DisputeWonDetails.class);

            MDC.put(GATEWAY_ACCOUNT_ID, disputeWonDetails.getGatewayAccountId());

            Service service = getService(disputeWonDetails.getGatewayAccountId());
            LedgerTransaction transaction = getTransaction(disputeWonEvent);
            List<UserEntity> serviceAdmins = userServices.getAdminUsersForService(service);

            if (shallSendDisputeUpdatedEmail(disputeWonEvent)) {
                Map<String, String> personalisation = Map.of(
                        "organisationName", service.getMerchantDetails().getName(),
                        "serviceName", service.getName(),
                        "serviceReference", transaction.getReference());
                if (!serviceAdmins.isEmpty()){
                    notificationService.sendStripeDisputeWonEmail(
                            serviceAdmins.stream().map(UserEntity::getEmail).collect(Collectors.toSet()),
                            personalisation
                    );
                    logger.info("Processed notification email for won dispute");
                } else {
                    throw new IllegalStateException(format("Service has no Admin users [external_id: %s]", service.getExternalId()));
                }
            } else {
                throw new IllegalArgumentException(format("Dispute won email sending is not yet enabled for [service_id: %s]", disputeWonEvent.getServiceId()));
            }
        } finally {
            tearDownMDC();
        }
    }

    private void handleDisputeLostMessage(Event disputeLostEvent) throws JsonProcessingException{
        try {
            setupMDC(disputeLostEvent);

            var disputeLostDetails = objectMapper.readValue(disputeLostEvent.getEventDetails(), DisputeLostDetails.class);

            MDC.put(GATEWAY_ACCOUNT_ID, disputeLostDetails.getGatewayAccountId());

            Service service = getService(disputeLostDetails.getGatewayAccountId());
            LedgerTransaction transaction = getTransaction(disputeLostEvent);
            List<UserEntity> serviceAdmins = userServices.getAdminUsersForService(service);

            if (shallSendDisputeUpdatedEmail(disputeLostEvent)) {
                Map<String, String> personalisation = Map.of(
                        "organisationName", service.getMerchantDetails().getName(),
                        "serviceName", service.getName(),
                        "serviceReference", transaction.getReference(),
                        "disputedAmount", convertPenceToPounds.apply(disputeLostDetails.getAmount()).toString(),
                        "disputeFee", convertPenceToPounds.apply(disputeLostDetails.getFee()).toString());

                if (!serviceAdmins.isEmpty()){
                    notificationService.sendStripeDisputeLostEmail(
                            serviceAdmins.stream().map(UserEntity::getEmail).collect(Collectors.toSet()),
                            personalisation
                    );
                    logger.info("Processed notification email for lost dispute");
                } else {
                    throw new IllegalStateException(format("Service has no Admin users [external_id: %s]", service.getExternalId()));
                }
            } else {
                throw new IllegalArgumentException(format("Dispute lost email sending is not yet enabled for [service_id: %s]", disputeLostEvent.getServiceId()));
            }
        } finally {
            tearDownMDC();
        }
    }

    private boolean shallSendDisputeUpdatedEmail(Event disputeLostEvent) {
        return (disputeLostEvent.getLive() &&
                notificationService.getEmailNotificationsForLivePaymentsDisputeUpdatesFrom()
                        .isBefore(Instant.now())) ||
                (!disputeLostEvent.getLive() &&
                        notificationService.getEmailNotificationsForTestPaymentsDisputeUpdatesFrom()
                                .isBefore(Instant.now()));
    }

    private void handleDisputeCreatedMessage(Event disputeCreatedEvent) throws JsonProcessingException {
        try {
            setupMDC(disputeCreatedEvent);

            var disputeCreatedDetails = objectMapper.readValue(disputeCreatedEvent.getEventDetails(), DisputeCreatedDetails.class);

            MDC.put(GATEWAY_ACCOUNT_ID, disputeCreatedDetails.getGatewayAccountId());

            Service service = getService(disputeCreatedDetails.getGatewayAccountId());
            LedgerTransaction transaction = getTransaction(disputeCreatedEvent);
            List<UserEntity> serviceAdmins = userServices.getAdminUsersForService(service);

            var epoch = disputeCreatedDetails.getEvidenceDueDate();
            String formattedDueDate = getZDTForEpoch(epoch).format(DATE_TIME_FORMATTER);
            String formattedPayDueDate = getPayDueByDateForEpoch(epoch).format(DATE_TIME_FORMATTER);

            Map<String, String> personalisation =  Map.of(
                    "serviceName", service.getName(),
                    "paymentExternalId", disputeCreatedEvent.getParentResourceExternalId(),
                    "serviceReference", transaction.getReference(),
                    "paymentAmount", convertPenceToPounds.apply(disputeCreatedDetails.getAmount()).toString(),
                    "disputeFee", convertPenceToPounds.apply(disputeCreatedDetails.getFee()).toString(),
                    "disputeEvidenceDueDate", formattedDueDate,
                    "sendEvidenceToPayDueDate", formattedPayDueDate
            );

            if (!serviceAdmins.isEmpty()) {
                notificationService.sendStripeDisputeCreatedEmail(
                        serviceAdmins.stream().map(UserEntity::getEmail).collect(Collectors.toSet()),
                        personalisation
                );
                logger.info("Processed notification email for disputed transaction");
            } else {
                throw new IllegalStateException(format("Service has no Admin users [external_id: %s]", service.getExternalId()));
            }
        } finally {
            tearDownMDC();
        }
    }

    private LedgerTransaction getTransaction(Event event) {
        return ledgerService.getTransaction(event.getParentResourceExternalId())
                .orElseThrow(() -> new IllegalArgumentException(format("Transaction not found [payment_external_id: %s]", event.getParentResourceExternalId())));
    }

    private Service getService(String gatewayAccountId) {
        return serviceFinder.byGatewayAccountId(gatewayAccountId)
                .orElseThrow(() -> new IllegalArgumentException(format("Service not found [gateway_account_id: %s]", gatewayAccountId)));
    }

    private void setupMDC(Event event) {
        MDC.put(GATEWAY_DISPUTE_ID, event.getResourceExternalId());
        MDC.put(PAYMENT_EXTERNAL_ID, event.getParentResourceExternalId());
        MDC.put(SERVICE_EXTERNAL_ID, event.getServiceId());
    }

    private void tearDownMDC() {
        List.of(PAYMENT_EXTERNAL_ID, GATEWAY_DISPUTE_ID, SERVICE_EXTERNAL_ID, GATEWAY_ACCOUNT_ID).forEach(MDC::remove);
    }
}
