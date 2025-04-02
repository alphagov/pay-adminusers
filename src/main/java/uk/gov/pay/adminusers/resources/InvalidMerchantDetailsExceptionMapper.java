package uk.gov.pay.adminusers.resources;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

public class InvalidMerchantDetailsExceptionMapper implements ExceptionMapper<InvalidMerchantDetailsException> {

    @Override
    public Response toResponse(InvalidMerchantDetailsException exception) {
        return Response.status(INTERNAL_SERVER_ERROR).build();
    }
}
