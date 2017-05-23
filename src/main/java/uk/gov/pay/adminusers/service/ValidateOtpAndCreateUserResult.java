package uk.gov.pay.adminusers.service;

import uk.gov.pay.adminusers.model.User;

import javax.ws.rs.WebApplicationException;

/**
 * This class is used to wrap the successful result or wrap thrown exception in result.
 * Similar to "Either" pattern: http://www.vavr.io/vavr-docs/#_either
 *
 * TODO: This class should be removed after refactoring of second factor authentication
 */
public class ValidateOtpAndCreateUserResult {

    private User user = null;

    private WebApplicationException error;

    public ValidateOtpAndCreateUserResult(User user) {
        this.user = user;
    }

    public ValidateOtpAndCreateUserResult(WebApplicationException error) {
        this.error = error;
    }

    public User getUser() {
        return user;
    }

    public WebApplicationException getError() {
        return error;
    }

    public boolean isError() {
        return error != null;
    }
}
