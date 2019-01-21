package uk.gov.pay.adminusers.exception;

public class GovUkPayAgreementNotSignedException extends ConflictException {
    public GovUkPayAgreementNotSignedException() {
        super("Nobody from this service is on record as having agreed to the legal terms");
    }
}
