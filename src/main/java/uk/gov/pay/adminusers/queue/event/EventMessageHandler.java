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
import uk.gov.pay.adminusers.queue.model.event.DisputeEvidenceSubmittedDetails;
import uk.gov.pay.adminusers.queue.model.event.DisputeLostDetails;
import uk.gov.pay.adminusers.queue.model.event.DisputeWonDetails;
import uk.gov.pay.adminusers.service.NotificationService;
import uk.gov.pay.adminusers.service.ServiceFinder;
import uk.gov.pay.adminusers.service.UserServices;
import uk.gov.service.payments.commons.queue.exception.QueueException;

import javax.inject.Inject;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.pay.adminusers.utils.currency.ConvertToCurrency.convertPenceToPounds;
import static uk.gov.pay.adminusers.utils.date.DisputeEvidenceDueByDateUtil.getPayDueByDate;
import static uk.gov.service.payments.logging.LoggingKeys.GATEWAY_ACCOUNT_ID;
import static uk.gov.service.payments.logging.LoggingKeys.LEDGER_EVENT_TYPE;
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
                EventType eventType = EventType.byType(message.getEvent().getEventType());

                logger.info("Retrieved event queue message with id [{}] for resource external id [{}]",
                        message.getQueueMessage().getMessageId(), message.getEvent().getResourceExternalId());

                switch (eventType) {
                    case DISPUTE_CREATED:
                        handleDisputeCreatedMessage(message.getEvent());
                        break;
                    case DISPUTE_EVIDENCE_SUBMITTED:
                        handleDisputeEvidenceSubmittedMessage(message.getEvent());
                        break;
                    case DISPUTE_LOST:
                        handleDisputeLostMessage(message.getEvent());
                        break;
                    case DISPUTE_WON:
                        handleDisputeWonMessage(message.getEvent());
                        break;
                    default:
                        logger.info("Unknown event type: {}", message.getEvent().getEventType());
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

    private void handleDisputeEvidenceSubmittedMessage(Event disputeEvidenceSubmittedEvent) throws JsonProcessingException {
        try {
            setupMDC(disputeEvidenceSubmittedEvent);

            var disputeEvidenceSubmittedDetails = objectMapper.readValue(disputeEvidenceSubmittedEvent.getEventDetails(), DisputeEvidenceSubmittedDetails.class);

            MDC.put(GATEWAY_ACCOUNT_ID, disputeEvidenceSubmittedDetails.getGatewayAccountId());

            if (shallSendDisputeUpdatedEmail(disputeEvidenceSubmittedEvent)) {
                Map<String, String> personalisation = getMinimumRequiredPersonalisation(
                        disputeEvidenceSubmittedDetails.getGatewayAccountId(),
                        disputeEvidenceSubmittedEvent.getParentResourceExternalId());

                sendEmailNotificationToServiceAdmins(disputeEvidenceSubmittedEvent.getEventType(),
                        disputeEvidenceSubmittedDetails.getGatewayAccountId(), personalisation);
            } else {
                logger.info("Dispute evidence submitted email sending is not yet enabled for [service_id: {}]", disputeEvidenceSubmittedEvent.getServiceId());
            }
        } finally {
            tearDownMDC();
        }
    }

    private void handleDisputeWonMessage(Event disputeWonEvent) throws JsonProcessingException {
        try {
            setupMDC(disputeWonEvent);

            var disputeWonDetails = objectMapper.readValue(disputeWonEvent.getEventDetails(), DisputeWonDetails.class);

            MDC.put(GATEWAY_ACCOUNT_ID, disputeWonDetails.getGatewayAccountId());

            if (shallSendDisputeUpdatedEmail(disputeWonEvent)) {
                Map<String, String> personalisation = getMinimumRequiredPersonalisation(disputeWonDetails.getGatewayAccountId(),
                        disputeWonEvent.getParentResourceExternalId());

                sendEmailNotificationToServiceAdmins(disputeWonEvent.getEventType(), disputeWonDetails.getGatewayAccountId(), personalisation);
            } else {
                logger.info("Dispute won email sending is not yet enabled for [service_id: {}]", disputeWonEvent.getServiceId());
            }
        } finally {
            tearDownMDC();
        }
    }

    private void handleDisputeLostMessage(Event disputeLostEvent) throws JsonProcessingException {
        try {
            setupMDC(disputeLostEvent);

            var disputeLostDetails = objectMapper.readValue(disputeLostEvent.getEventDetails(), DisputeLostDetails.class);

            MDC.put(GATEWAY_ACCOUNT_ID, disputeLostDetails.getGatewayAccountId());

            if (shallSendDisputeUpdatedEmail(disputeLostEvent)) {
                Map<String, String> personalisation = getPersonalisationForDisputeLost(disputeLostDetails,
                        disputeLostEvent.getParentResourceExternalId());

                sendEmailNotificationToServiceAdmins(disputeLostEvent.getEventType(), disputeLostDetails.getGatewayAccountId(),
                        personalisation);

            } else {
                logger.info("Dispute lost email sending is not yet enabled for [service_id: {}]", disputeLostEvent.getServiceId());
            }
        } finally {
            tearDownMDC();
        }
    }

    private Map<String, String> getMinimumRequiredPersonalisation(String gatewayAccountId, String parentResourceExternalId) {
        Service service = getService(gatewayAccountId);
        LedgerTransaction transaction = getTransaction(parentResourceExternalId);

        String organisationName = (service.getMerchantDetails() != null && service.getMerchantDetails().getName() != null) ?
                service.getMerchantDetails().getName() : service.getName();

        return new HashMap<>(Map.of("organisationName", organisationName,
                "serviceName", service.getName(),
                "serviceReference", transaction.getReference()));
    }

    private Map<String, String> getPersonalisationForDisputeLost(DisputeLostDetails details, String parentResourceExternalId) {
        var personalisation = getMinimumRequiredPersonalisation(details.getGatewayAccountId(), parentResourceExternalId);

        if (details.getFee() != null) {
            personalisation.put("disputedAmount", convertPenceToPounds.apply(details.getAmount()).toString());
            personalisation.put("disputeFee", convertPenceToPounds.apply(details.getFee()).toString());
        }

        return personalisation;
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

            String formattedDueDate = disputeCreatedDetails.getEvidenceDueDate().format(DATE_TIME_FORMATTER);
            String formattedPayDueDate = getPayDueByDate(disputeCreatedDetails.getEvidenceDueDate()).format(DATE_TIME_FORMATTER);

            Map<String, String> personalisation = getMinimumRequiredPersonalisation(disputeCreatedDetails.getGatewayAccountId(),
                    disputeCreatedEvent.getParentResourceExternalId());

            personalisation.put("paymentExternalId", disputeCreatedEvent.getParentResourceExternalId());
            personalisation.put("disputedAmount", convertPenceToPounds.apply(disputeCreatedDetails.getAmount()).toString());
            personalisation.put("paymentAmount", convertPenceToPounds.apply(disputeCreatedDetails.getAmount()).toString());
            personalisation.put("disputeEvidenceDueDate", formattedDueDate);
            personalisation.put("sendEvidenceToPayDueDate", formattedPayDueDate);

            sendEmailNotificationToServiceAdmins(disputeCreatedEvent.getEventType(), disputeCreatedDetails.getGatewayAccountId(), personalisation);
        } finally {
            tearDownMDC();
        }
    }

    private LedgerTransaction getTransaction(String parentResourceExternalId) {
        return ledgerService.getTransaction(parentResourceExternalId)
                .orElseThrow(() -> new IllegalArgumentException(format("Transaction not found [payment_external_id: %s]",
                        parentResourceExternalId)));
    }

    private Service getService(String gatewayAccountId) {
        return serviceFinder.byGatewayAccountId(gatewayAccountId)
                .orElseThrow(() -> new IllegalArgumentException(format("Service not found [gateway_account_id: %s]", gatewayAccountId)));
    }

    private void sendEmailNotificationToServiceAdmins(String eventType, String gatewayAccountId,
                                                      Map<String, String> personalisation) {
        Service service = getService(gatewayAccountId);
        List<UserEntity> serviceAdmins = userServices.getAdminUsersForService(service.getId());

        if (!serviceAdmins.isEmpty()) {
            sendDisputeEmailForEvent(eventType, serviceAdmins.stream().map(UserEntity::getEmail).collect(Collectors.toSet()),
                    personalisation);
            logger.info("Processed notification email for disputed transaction");
        } else {
            throw new IllegalStateException(format("Service has no Admin users [external_id: %s]", service.getExternalId()));
        }
    }

    private void sendDisputeEmailForEvent(String eventType, Set<String> adminEmails, Map<String, String> personalisation) {
        EventType disputeEventType = EventType.valueOf(eventType.toUpperCase());
        switch (disputeEventType) {
            case DISPUTE_CREATED:
                notificationService.sendStripeDisputeCreatedEmail(adminEmails, personalisation);
                break;
            case DISPUTE_LOST:
                notificationService.sendStripeDisputeLostEmail(adminEmails, personalisation);
                break;
            case DISPUTE_WON:
                notificationService.sendStripeDisputeWonEmail(adminEmails, personalisation);
                break;
            case DISPUTE_EVIDENCE_SUBMITTED:
                notificationService.sendStripeDisputeEvidenceSubmittedEmail(adminEmails, personalisation);
                break;
            default:
                logger.warn("Unknown event type: {}", eventType);
        }
    }

    private void setupMDC(Event event) {
        MDC.put("dispute_external_id", event.getResourceExternalId());
        MDC.put(PAYMENT_EXTERNAL_ID, event.getParentResourceExternalId());
        MDC.put(SERVICE_EXTERNAL_ID, event.getServiceId());
        MDC.put(LEDGER_EVENT_TYPE, event.getEventType());
    }

    private void tearDownMDC() {
        List.of(PAYMENT_EXTERNAL_ID, SERVICE_EXTERNAL_ID, GATEWAY_ACCOUNT_ID, "dispute_external_id", LEDGER_EVENT_TYPE).forEach(MDC::remove);
    }
}
