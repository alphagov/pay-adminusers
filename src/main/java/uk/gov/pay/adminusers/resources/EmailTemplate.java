package uk.gov.pay.adminusers.resources;

import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;

public enum EmailTemplate {
    MANDATE_CANCELLED,
    MANDATE_FAILED,
    PAYMENT_CONFIRMED;

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
