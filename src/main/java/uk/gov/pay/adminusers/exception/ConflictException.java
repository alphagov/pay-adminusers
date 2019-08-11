package uk.gov.pay.adminusers.exception;

class ConflictException extends RuntimeException {
    ConflictException(String message) {
        super(message);
    }
}
