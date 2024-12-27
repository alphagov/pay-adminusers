package uk.gov.pay.adminusers.resources;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

public class InvalidMerchantDetailsExceptionMapper implements ExceptionMapper<InvalidMerchantDetailsException> {

    @Override
    public Response toResponse(InvalidMerchantDetailsException exception) {
        return Response.status(INTERNAL_SERVER_ERROR).build();
    }
}
