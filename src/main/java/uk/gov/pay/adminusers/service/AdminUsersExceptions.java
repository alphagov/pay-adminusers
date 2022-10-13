package uk.gov.pay.adminusers.service;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

public class AdminUsersExceptions {

    public static WebApplicationException undefinedRoleException(String roleName) {
        String error = format("role [%s] not recognised", roleName);
        return buildWebApplicationException(error, BAD_REQUEST.getStatusCode());
    }

    public static WebApplicationException conflictingUsername(String username) {
        String error = format("username [%s] already exists", username);
        return buildWebApplicationException(error, CONFLICT.getStatusCode());
    }

    public static WebApplicationException conflictingEmail(String email) {
        String error = format("email [%s] already exists", email);
        return buildWebApplicationException(error, CONFLICT.getStatusCode());
    }

    public static WebApplicationException conflictingInvite(String email) {
        String error = format("invite with email [%s] already exists", email);
        return buildWebApplicationException(error, CONFLICT.getStatusCode());
    }

    public static WebApplicationException userAlreadyInService(String userExternalId, String serviceExternalId) {
        String error = format("user [%s] already in service [%s]", userExternalId, serviceExternalId);
        return buildWebApplicationException(error, PRECONDITION_FAILED.getStatusCode());
    }

    public static WebApplicationException conflictingServiceGatewayAccountsForUser() {
        return buildWebApplicationException("List of gateway accounts not matching one of the existing services", CONFLICT.getStatusCode());
    }

    public static WebApplicationException conflictingServiceRoleForUser(String userExternalId, String serviceExternalId) {
        String error = format("Cannot assign service role. user [%s] already got access to service [%s].", userExternalId, serviceExternalId);
        return buildWebApplicationException(error, CONFLICT.getStatusCode());
    }

    public static WebApplicationException conflictingServiceForUser(String userExternalId, String serviceExternalId) {
        String error = format("user [%s] does not belong to service [%s]", userExternalId, serviceExternalId);
        return buildWebApplicationException(error, CONFLICT.getStatusCode());
    }

    public static WebApplicationException serviceDoesNotExistError(String serviceId) {
        String error = format("Service %s provided does not exist", serviceId);
        return buildWebApplicationException(error, BAD_REQUEST.getStatusCode());
    }

    public static WebApplicationException notFoundException() {
        return new WebApplicationException(Response.status(NOT_FOUND.getStatusCode()).build());
    }

    public static WebApplicationException notFoundInviteException(String inviteCode) {
        String error = format("Invite for code %s provided does not exist", inviteCode);
        return buildWebApplicationException(error, NOT_FOUND.getStatusCode());
    }

    public static WebApplicationException invalidOtpAuthCodeInviteException(String inviteCode) {
        String error = format("Invite for code %s provided invalid otp auth code", inviteCode);
        return buildWebApplicationException(error, UNAUTHORIZED.getStatusCode());
    }

    public static WebApplicationException inviteLockedException(String inviteCode) {
        String error = format("Invite for code %s locked due to too many otp auth attempts", inviteCode);
        return buildWebApplicationException(error, GONE.getStatusCode());
    }

    public static WebApplicationException forbiddenOperationException(String externalId, String operation, String externalServiceId) {
        String error = format("user [%s] not authorised to perform operation [%s] in service [%s]", externalId, operation, externalServiceId);
        return buildWebApplicationException(error, FORBIDDEN.getStatusCode());
    }

    public static WebApplicationException internalServerError(String message) {
        return buildWebApplicationException(message, INTERNAL_SERVER_ERROR.getStatusCode());
    }

    public static WebApplicationException adminRoleLimitException(int adminLimit) {
        String error = format("Service admin limit reached. At least %d admin(s) required", adminLimit);
        return buildWebApplicationException(error, PRECONDITION_FAILED.getStatusCode());
    }

    public static WebApplicationException conflictingServiceGatewayAccounts(List<String> gatewayAccountsIds) {
        String error = format("One or more of the following gateway account ids has already assigned to another service: [%s]", String.join(",", gatewayAccountsIds));
        return buildWebApplicationException(error, CONFLICT.getStatusCode());
    }

    public static WebApplicationException userNotificationError(Exception cause) {
        return buildWebApplicationException("error sending user notification", INTERNAL_SERVER_ERROR.getStatusCode(), cause);
    }
    
    public static WebApplicationException otpKeyMissingException(String userExternalId) {
        String error = format("Attempted to send a 2FA token attempted for user without an OTP key [%s]", userExternalId);
        return buildWebApplicationException(error, BAD_REQUEST.getStatusCode());
    }

    public static WebApplicationException userDoesNotHaveTelephoneNumberError(String userExternalId) {
        String error = format("Unable to send second factor code as user [%s] does not have a telephone number set", userExternalId);
        return buildWebApplicationException(error, PRECONDITION_FAILED.getStatusCode());
    }
    
    public static WebApplicationException cannotResetSecondFactorToSmsError(String userExternalId) {
        String error = format("Unable to reset second factor method to SMS as user [%s] does not have a telephone number set", userExternalId);
        return buildWebApplicationException(error, PRECONDITION_FAILED.getStatusCode());
    }

    private static WebApplicationException buildWebApplicationException(String error, int status) {
        return buildWebApplicationException(error, status, null);
    }
    
    private static WebApplicationException buildWebApplicationException(String error, int status, Exception cause) {
        Response response = Response.status(status)
                .entity(Map.of("errors", List.of(error)))
                .build();
        return new WebApplicationException(cause, response);
    }

    public static WebApplicationException invalidPublicSectorEmail(String email) {
        String error = format("Email [%s] is not a valid public sector email", email);
        return buildWebApplicationException(error, FORBIDDEN.getStatusCode());
    }
}
