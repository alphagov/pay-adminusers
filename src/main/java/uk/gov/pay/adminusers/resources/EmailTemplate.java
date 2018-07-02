package uk.gov.pay.adminusers.resources;

import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;

public enum EmailTemplate {
    ON_DEMAND_MANDATE_CREATED,
    ONE_OFF_MANDATE_CREATED,
    MANDATE_CANCELLED,
    MANDATE_FAILED,
    ONE_OFF_PAYMENT_CONFIRMED,
    ON_DEMAND_PAYMENT_CONFIRMED,
    PAYMENT_FAILED;

    private static final Logger LOGGER = PayLoggerFactory.getLogger(EmailTemplate.class);

    public static EmailTemplate fromString(String type) {
        for (EmailTemplate typeEnum : values()) {
            if (typeEnum.toString().equalsIgnoreCase(type)) {
                return typeEnum;
            }
        }
        LOGGER.warn("Unknown email template: {}", type);
        return null;
    }
}
