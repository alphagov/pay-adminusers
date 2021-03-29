package uk.gov.pay.adminusers.filters;

import org.slf4j.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;
import java.util.List;

import static uk.gov.service.payments.logging.LoggingKeys.SERVICE_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.USER_EXTERNAL_ID;

public class LoggingMDCResponseFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        List.of(SERVICE_EXTERNAL_ID, USER_EXTERNAL_ID).forEach(MDC::remove);
    }
}
