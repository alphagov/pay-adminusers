package uk.gov.pay.adminusers.resources;

public class InvalidMerchantDetailsException extends Exception {
    public InvalidMerchantDetailsException(String message) {
        super(message);
    }
}
