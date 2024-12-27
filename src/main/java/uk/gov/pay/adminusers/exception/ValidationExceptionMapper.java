package uk.gov.pay.adminusers.exception;

import uk.gov.service.payments.commons.api.exception.ValidationException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import java.util.Map;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(ValidationException exception) {
        return Response.status(BAD_REQUEST)
                .entity(Map.of("errors", exception.getErrors()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

}
