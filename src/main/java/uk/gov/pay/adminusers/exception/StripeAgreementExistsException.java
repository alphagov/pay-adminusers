package uk.gov.pay.adminusers.exception;

public class StripeAgreementExistsException extends ConflictException {
    public StripeAgreementExistsException() {
        super("Stripe agreement information is already stored for this service");
    }
}
