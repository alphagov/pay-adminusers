package uk.gov.pay.adminusers.service;

import com.google.common.collect.ImmutableMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class AdminUsersExceptions {

    public static WebApplicationException undefinedRoleException(String roleName) {
        String error = format("role [%s] not recognised", roleName);
        Map<String, List<String>> errors = ImmutableMap.of("errors", asList(error));
        Response response = Response.status(BAD_REQUEST)
                .entity(errors)
                .build();
        return new WebApplicationException(response);
    }
}
