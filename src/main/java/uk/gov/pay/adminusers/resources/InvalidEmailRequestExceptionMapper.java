package uk.gov.pay.adminusers.resources;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class InvalidEmailRequestExceptionMapper implements ExceptionMapper<InvalidEmailRequestException> {

    @Override
    public Response toResponse(InvalidEmailRequestException exception) {
        return Response.status(BAD_REQUEST).build();
    }

}
