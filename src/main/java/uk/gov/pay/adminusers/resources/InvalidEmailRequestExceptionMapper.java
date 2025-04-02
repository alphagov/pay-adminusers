package uk.gov.pay.adminusers.resources;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

public class InvalidEmailRequestExceptionMapper implements ExceptionMapper<InvalidEmailRequestException> {

    @Override
    public Response toResponse(InvalidEmailRequestException exception) {
        return Response.status(BAD_REQUEST).build();
    }

}
