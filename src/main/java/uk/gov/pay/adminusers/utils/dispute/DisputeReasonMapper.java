package uk.gov.pay.adminusers.utils.dispute;

public class DisputeReasonMapper {

    public static String mapToNotifyEmail(String stripeReason) {
        return switch (stripeReason) {
            case null -> "unknown";
            case String reason when reason.isBlank() -> "unknown";
            case "duplicate", "fraudulent", "general" -> stripeReason;
            case "credit_not_processed" -> "credit not processed";
            case "product_not_received" -> "product not received";
            case "product_unacceptable" -> "product unacceptable";
            case "subscription_canceled" -> "subscription cancelled";
            case "unrecognized" -> "unrecognised";
            default -> "other";
        };
    }
}
