package uk.gov.pay.adminusers.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

public class ServiceNotFoundExceptionMapper implements ExceptionMapper<ServiceNotFoundException> {

    @Override
    public Response toResponse(ServiceNotFoundException exception) {
        return Response.status(NOT_FOUND).build();
    }

}
