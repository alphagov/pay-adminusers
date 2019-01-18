package uk.gov.pay.adminusers.exception;

public class StripeAgreementAlreadyExistsException extends ConflictException {
    
    public StripeAgreementAlreadyExistsException() {
        super("Stripe agreement information is already stored for this service");
    }
}
