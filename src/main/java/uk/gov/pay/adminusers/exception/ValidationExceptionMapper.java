package uk.gov.pay.adminusers.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(ValidationException exception) {
        return Response.status(BAD_REQUEST)
                .entity(exception.getErrors())
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

}
