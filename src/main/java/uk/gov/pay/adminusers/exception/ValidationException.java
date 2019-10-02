package uk.gov.pay.adminusers.exception;

import uk.gov.pay.adminusers.utils.Errors;

public class ValidationException extends Throwable {

    private Errors errors;

    public ValidationException(Errors errors) {
        super();
        this.errors = errors;
    }

    public Errors getErrors() {
        return errors;
    }

}
