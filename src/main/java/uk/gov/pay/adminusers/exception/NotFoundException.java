package uk.gov.pay.adminusers.exception;

class NotFoundException extends RuntimeException {
    NotFoundException(String message) {
        super(message);
    }
}
