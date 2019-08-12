package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import liquibase.exception.ServiceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.resources.EmailTemplate;
import uk.gov.pay.adminusers.resources.InvalidMerchantDetailsException;
import uk.gov.pay.adminusers.utils.CountryConverter;
import uk.gov.pay.commons.model.SupportedLanguage;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static uk.gov.pay.adminusers.model.PaymentType.DIRECT_DEBIT;

public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private static final String SERVICE_NAME_KEY = "service name";
    private static final String ORGANISATION_NAME_KEY = "organisation name";
    private static final String ORGANISATION_PHONE_NUMBER_KEY = "organisation phone number";
    private static final String ORGANISATION_ADDRESS_KEY = "organisation address";
    private static final String ORGANISATION_EMAIL_ADDRESS_KEY = "organisation email address";

    private final NotificationService notificationService;
    private final ServiceDao serviceDao;
    private final CountryConverter countryConverter;

    @Inject
    public EmailService(NotificationService notificationService,
                        CountryConverter countryConverter,
                        ServiceDao serviceDao) {
        this.serviceDao = serviceDao;
        this.notificationService = notificationService;
        this.countryConverter = countryConverter;
    }

    private String formatMerchantAddress(MerchantDetailsEntity merchantDetails) {
        StringJoiner merchantAddress = new StringJoiner(", ", "", "");
        merchantAddress.add(merchantDetails.getAddressLine1());
        if (!StringUtils.isBlank(merchantDetails.getAddressLine2())) {
            merchantAddress.add(merchantDetails.getAddressLine2());
        }
        merchantAddress.add(merchantDetails.getAddressCity());
        merchantAddress.add(merchantDetails.getAddressPostcode());
        countryConverter.getCountryNameFrom(merchantDetails.getAddressCountryCode())
                .ifPresent(merchantAddress::add);
        return merchantAddress.toString();
    }

    private boolean isMissingMandatoryFields(MerchantDetailsEntity merchantDetails) {
        return Stream.of(
                merchantDetails.getName(),
                merchantDetails.getTelephoneNumber(),
                merchantDetails.getAddressLine1(),
                merchantDetails.getAddressCity(),
                merchantDetails.getAddressCountryCode(),
                merchantDetails.getAddressPostcode(),
                merchantDetails.getEmail()
        ).anyMatch(StringUtils::isBlank);
    }

    private Map<EmailTemplate, StaticEmailContent> getTemplateMappingsFor(String gatewayAccountId) throws InvalidMerchantDetailsException {
        ServiceEntity service = getServiceFor(gatewayAccountId);
        MerchantDetailsEntity merchantDetails = service.getMerchantDetailsEntity();

        if (merchantDetails == null) {
            LOGGER.error("Merchant details are empty: can't send email for account {}", gatewayAccountId);
            throw new InvalidMerchantDetailsException("Merchant details are empty: can't send email for account " + gatewayAccountId);
        } else if (isMissingMandatoryFields(merchantDetails)) {
            LOGGER.error("Merchant details are missing mandatory fields: can't send email for account {}", gatewayAccountId);
            throw new InvalidMerchantDetailsException("Merchant details are missing mandatory fields: can't send email for account " + gatewayAccountId);
        }

        final Map<String, String> personalisation = Map.of(
                SERVICE_NAME_KEY, service.getServiceNames().get(SupportedLanguage.ENGLISH).getName(),
                ORGANISATION_NAME_KEY, merchantDetails.getName(),
                ORGANISATION_ADDRESS_KEY, formatMerchantAddress(merchantDetails),
                ORGANISATION_PHONE_NUMBER_KEY, merchantDetails.getTelephoneNumber(),
                ORGANISATION_EMAIL_ADDRESS_KEY, merchantDetails.getEmail()
        );

        return Map.of(
                EmailTemplate.ONE_OFF_PAYMENT_CONFIRMED, new StaticEmailContent(
                        notificationService.getNotifyDirectDebitConfiguration().getOneOffMandateAndPaymentCreatedEmailTemplateId(),
                        personalisation),
                EmailTemplate.ON_DEMAND_PAYMENT_CONFIRMED, new StaticEmailContent(
                        notificationService.getNotifyDirectDebitConfiguration().getOnDemandPaymentConfirmedEmailTemplateId(),
                        personalisation),
                EmailTemplate.PAYMENT_FAILED, new StaticEmailContent(
                        notificationService.getNotifyDirectDebitConfiguration().getPaymentFailedEmailTemplateId(),
                        personalisation),
                EmailTemplate.MANDATE_CANCELLED, new StaticEmailContent(
                        notificationService.getNotifyDirectDebitConfiguration().getMandateCancelledEmailTemplateId(),
                        personalisation),
                EmailTemplate.MANDATE_FAILED, new StaticEmailContent(
                        notificationService.getNotifyDirectDebitConfiguration().getMandateFailedEmailTemplateId(),
                        personalisation),
                EmailTemplate.ON_DEMAND_MANDATE_CREATED, new StaticEmailContent(
                        notificationService.getNotifyDirectDebitConfiguration().getOnDemandMandateCreatedEmailTemplateId(),
                        personalisation),
                EmailTemplate.ONE_OFF_MANDATE_CREATED, new StaticEmailContent(
                        notificationService.getNotifyDirectDebitConfiguration().getOneOffMandateAndPaymentCreatedEmailTemplateId(),
                        personalisation));
    }

    private ServiceEntity getServiceFor(String gatewayAccountId) {
        return serviceDao.findByGatewayAccountId(gatewayAccountId)
                .orElseThrow(() -> new ServiceNotFoundException("Service not found"));
    }

    public CompletableFuture<String> sendEmail(String email, String gatewayAccountId, EmailTemplate template, Map<String, String> dynamicContent) throws InvalidMerchantDetailsException {
        StaticEmailContent staticEmailContent = getTemplateMappingsFor(gatewayAccountId).get(template);
        Map<String, String> staticContent = new HashMap<>(staticEmailContent.getPersonalisation());
        staticContent.putAll(dynamicContent);
        LOGGER.info("Sending direct debit email for " + template.toString());
        return notificationService.sendEmailAsync(DIRECT_DEBIT, staticEmailContent.getTemplateId(), email, staticContent);
    }
}
