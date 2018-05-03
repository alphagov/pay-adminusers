package uk.gov.pay.adminusers.service;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import liquibase.exception.ServiceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.resources.EmailTemplate;
import uk.gov.pay.adminusers.resources.InvalidMerchantDetailsException;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static uk.gov.pay.adminusers.model.PaymentType.DIRECT_DEBIT;

public class EmailService {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(EmailService.class);

    private NotificationService notificationService;
    private ServiceDao serviceDao;

    @Inject
    public EmailService(NotificationService notificationService, ServiceDao serviceDao) {
        this.serviceDao = serviceDao;
        this.notificationService = notificationService;
    }

    private String formatMerchantAddress(MerchantDetailsEntity merchantDetails) {
        StringJoiner merchantAddress = new StringJoiner(", ", "", "");
        merchantAddress.add(merchantDetails.getName());
        merchantAddress.add(merchantDetails.getAddressLine1());
        if (!StringUtils.isBlank(merchantDetails.getAddressLine2())) {
            merchantAddress.add(merchantDetails.getAddressLine2());
        }
        merchantAddress.add(merchantDetails.getAddressCity());
        merchantAddress.add(merchantDetails.getAddressPostcode());
        merchantAddress.add(merchantDetails.getAddressCountry());
        return merchantAddress.toString();
    }

    private boolean isMissingMandatoryFields(MerchantDetailsEntity merchantDetails) {
        return Stream.of(
                merchantDetails.getName(),
                merchantDetails.getTelephoneNumber(),
                merchantDetails.getAddressLine1(),
                merchantDetails.getAddressCity(),
                merchantDetails.getAddressCountry(),
                merchantDetails.getAddressPostcode(),
                merchantDetails.getEmail()
        ).anyMatch(StringUtils::isBlank);
    }

    private Map<EmailTemplate, StaticEmailContent> getTemplateMappingsFor(String gatewayAccountId) throws InvalidMerchantDetailsException {
        ServiceEntity service = getServiceFor(gatewayAccountId);
        MerchantDetailsEntity merchantDetails = service.getMerchantDetailsEntity();

        if (merchantDetails == null || isMissingMandatoryFields(merchantDetails)) {
            LOGGER.error("Merchant details are empty: can't send email for account {}", gatewayAccountId);
            throw new InvalidMerchantDetailsException("Merchant details are empty: can't send email for account " + gatewayAccountId);
        }
        String merchantAddress = formatMerchantAddress(merchantDetails);

        return ImmutableMap.of(
                EmailTemplate.PAYMENT_CONFIRMED, new StaticEmailContent(
                        notificationService.getNotifyConfiguration().getPaymentConfirmedTemplateId(),
                        ImmutableMap.of(
                                "service name", service.getName(),
                                "merchant address", merchantAddress,
                                "merchant phone number", merchantDetails.getTelephoneNumber(),
                                "merchant email", merchantDetails.getEmail()
                        )
                )
                ,
                EmailTemplate.PAYMENT_FAILED, new StaticEmailContent(
                        notificationService.getNotifyConfiguration().getPaymentFailedTemplateId(),
                        ImmutableMap.of(
                                "org name", merchantDetails.getName(),
                                "org phone", merchantDetails.getTelephoneNumber()
                        )
                ),
                EmailTemplate.MANDATE_CANCELLED, new StaticEmailContent(
                        notificationService.getNotifyConfiguration().getMandateCancelledTemplateId(),
                        ImmutableMap.of(
                                "org name", merchantDetails.getName(),
                                "org phone", merchantDetails.getTelephoneNumber(),
                                "merchant email", merchantDetails.getEmail()
                        )
                ),
                EmailTemplate.MANDATE_FAILED, new StaticEmailContent(
                        notificationService.getNotifyConfiguration().getMandateFailedTemplateId(),
                        ImmutableMap.of(
                                "org name", merchantDetails.getName(),
                                "org phone", merchantDetails.getTelephoneNumber(),
                                "merchant email", merchantDetails.getEmail()
                        ))
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
