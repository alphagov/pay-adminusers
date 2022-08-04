package uk.gov.pay.adminusers.utils.dispute;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class DisputeReasonMapper {

    public static String mapToNotifyEmail(String stripeReason) {
        if (isBlank(stripeReason)) {
            return "unknown";
        }
        switch (stripeReason) {
            case "duplicate":
            case "fraudulent":
            case "general":
                return stripeReason;
            case "credit_not_processed":
                return "credit not processed";
            case "product_not_received":
                return "product not received";
            case "product_unacceptable":
                return "product unacceptable";
            case "subscription_canceled":
                return "subscription cancelled";
            case "unrecognized":
                return "unrecognised";
            default:
                return "other";
        }
    }
}
