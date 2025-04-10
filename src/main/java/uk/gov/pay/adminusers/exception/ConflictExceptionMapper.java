package uk.gov.pay.adminusers.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

public class ConflictExceptionMapper implements ExceptionMapper<ConflictException> {

    @Override
    public Response toResponse(ConflictException exception) {
        return Response
                .status(Response.Status.CONFLICT)
                .entity(Map.of("errors", List.of(exception.getMessage())))
                .type(APPLICATION_JSON_TYPE)
                .build();
    }
}
