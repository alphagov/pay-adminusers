package uk.gov.pay.adminusers.filters;

import org.slf4j.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;
import java.util.Optional;

import static uk.gov.service.payments.logging.LoggingKeys.SERVICE_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.USER_EXTERNAL_ID;

public class LoggingMDCRequestFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        getPathParameterFromRequest("serviceExternalId", requestContext)
                .ifPresent(serviceExternalId -> MDC.put(SERVICE_EXTERNAL_ID, serviceExternalId));

        getPathParameterFromRequest("userExternalId", requestContext)
                .ifPresent(userExternalId -> MDC.put(USER_EXTERNAL_ID, userExternalId));
    }

    private Optional<String> getPathParameterFromRequest(String parameterName, ContainerRequestContext requestContext) {
        return Optional.ofNullable(requestContext.getUriInfo().getPathParameters().getFirst(parameterName));
    }
}
