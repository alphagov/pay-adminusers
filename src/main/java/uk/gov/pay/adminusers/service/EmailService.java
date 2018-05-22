package uk.gov.pay.adminusers.service;

import static uk.gov.pay.adminusers.model.PaymentType.DIRECT_DEBIT;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import liquibase.exception.ServiceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.resources.EmailTemplate;
import uk.gov.pay.adminusers.resources.InvalidMerchantDetailsException;
import uk.gov.pay.adminusers.utils.CountryConverter;

public class EmailService {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(EmailService.class);
    public static final String SERVICE_NAME_KEY = "service name";
    public static final String ORGANISATION_NAME_KEY = "organisation name";
    public static final String ORGANISATION_PHONE_NUMBER_KEY = "organisation phone number";
    public static final String ORGANISATION_ADDRESS_KEY = "organisation address";
    public static final String ORGANISATION_EMAIL_ADDRESS_KEY = "organisation email address";

    private final NotificationService notificationService;
    private final ServiceDao serviceDao;
    private final CountryConverter countryConverter;
    @Inject
    public EmailService(NotificationService notificationService, CountryConverter countryConverter, ServiceDao serviceDao) {
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

        ImmutableMap<String, String> personalisation = ImmutableMap.of(
                SERVICE_NAME_KEY, service.getName(),
                ORGANISATION_NAME_KEY, merchantDetails.getName(),
                ORGANISATION_ADDRESS_KEY, formatMerchantAddress(merchantDetails),
                ORGANISATION_PHONE_NUMBER_KEY, merchantDetails.getTelephoneNumber(),
                ORGANISATION_EMAIL_ADDRESS_KEY, merchantDetails.getEmail()
        );
        return ImmutableMap.of(
                EmailTemplate.PAYMENT_CONFIRMED, new StaticEmailContent(
                        notificationService.getNotifyConfiguration().getPaymentConfirmedTemplateId(),
                        personalisation
                ),
                EmailTemplate.PAYMENT_FAILED, new StaticEmailContent(
                        notificationService.getNotifyConfiguration().getPaymentFailedTemplateId(),
                        personalisation
                ),
                EmailTemplate.MANDATE_CANCELLED, new StaticEmailContent(
                        notificationService.getNotifyConfiguration().getMandateCancelledTemplateId(),
                        personalisation
                ),
                EmailTemplate.MANDATE_FAILED, new StaticEmailContent(
                        notificationService.getNotifyConfiguration().getMandateFailedTemplateId(),
                        personalisation
                )
        );
    }

    private ServiceEntity getServiceFor(String gatewayAccountId) {
        return serviceDao.findByGatewayAccountId(gatewayAccountId)
                .orElseThrow(() -> new ServiceNotFoundException("Service not found"));
    }

    public CompletableFuture<String> sendEmail(String email, String gatewayAccountId, EmailTemplate template, Map<String, String> dynamicContent) throws InvalidMerchantDetailsException {
        Map<EmailTemplate, StaticEmailContent> templateMappingsFor = getTemplateMappingsFor(gatewayAccountId);
        Map<String, String> staticContent = templateMappingsFor.get(template).getPersonalisation();
        Map<String, String> allContent = new HashMap<>();
        allContent.putAll(staticContent);
        allContent.putAll(dynamicContent);
        LOGGER.info("Sending direct debit email for " + template.toString());
        return notificationService.sendEmailAsync(DIRECT_DEBIT, templateMappingsFor.get(template).getTemplateId(), email, allContent);
    }
}
