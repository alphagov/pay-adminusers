package uk.gov.pay.adminusers.exception;

import uk.gov.pay.adminusers.utils.Errors;

public class ValidationException extends Throwable {

    private final Errors errors;

    public ValidationException(Errors errors) {
        this.errors = errors;
    }

    public Errors getErrors() {
        return errors;
    }

}
