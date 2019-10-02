package uk.gov.pay.adminusers.resources;

public class InvalidEmailRequestException extends Exception {

    public InvalidEmailRequestException(String message, Exception cause) {
        super(message, cause);
    }

}
