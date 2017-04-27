package uk.gov.pay.adminusers.service;

import com.google.common.collect.ImmutableMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.Response.Status.*;

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

    public static WebApplicationException conflictingServiceGatewayAccounts() {
        String error = format("List of gateway accounts not matching one of the existing services");
        return buildWebApplicationException(error, CONFLICT.getStatusCode());
    }

    public static WebApplicationException conflictingServiceForUser(Integer userId, Integer serviceId) {
        String error = format("user [%d] does not belong to service [%d]", userId, serviceId);
        return buildWebApplicationException(error, CONFLICT.getStatusCode());
    }

    public static WebApplicationException notFoundServiceError(String serviceId) {
        String error = format("Service %s provided does not exist", serviceId);
        return buildWebApplicationException(error, BAD_REQUEST.getStatusCode());
    }

    public static WebApplicationException notFoundException() {
        return new WebApplicationException(Response.status(NOT_FOUND.getStatusCode()).build());
    }

    public static WebApplicationException userLockedException(String username) {
        String error = format("user [%s] locked due to too many login attempts", username);
        return buildWebApplicationException(error, UNAUTHORIZED.getStatusCode());
    }

    public static WebApplicationException forbiddenOperationException(String externalId, String operation, int serviceId) {
        String error = format("user [%s] not authorised to perform operation [%s] in service [%d]", externalId, operation, serviceId);
        return buildWebApplicationException(error, FORBIDDEN.getStatusCode());
    }

    public static WebApplicationException internalServerError(String message) {
        return buildWebApplicationException(message, INTERNAL_SERVER_ERROR.getStatusCode());
    }

    public static WebApplicationException adminRoleLimitException(int adminLimit) {
        String error = format("Service admin limit reached. At least %d admin(s) required", adminLimit);
        return buildWebApplicationException(error, PRECONDITION_FAILED.getStatusCode());
    }

    public static RuntimeException userNotificationError(Exception e) {
        return new RuntimeException("error sending user notification", e);
    }

    public static WebApplicationException resourceHasExpired(){
        String error = format("Resource has expired");
        return buildWebApplicationException(error, GONE.getStatusCode());
    }

    private static WebApplicationException buildWebApplicationException(String error, int status) {
        Map<String, List<String>> errors = ImmutableMap.of("errors", asList(error));
        Response response = Response.status(status)
                .entity(errors)
                .build();
        return new WebApplicationException(response);
    }
}
