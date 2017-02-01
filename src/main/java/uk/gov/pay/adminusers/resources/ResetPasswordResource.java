package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.service.ResetPasswordService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;

@Path("/")
public class ResetPasswordResource {

    private static final String RESET_PASSWORD_RESOURCE = "/v1/api/reset-password";
    private final ResetPasswordService resetPasswordService;

    @Inject
    public ResetPasswordResource(ResetPasswordService resetPasswordService) {
        this.resetPasswordService = resetPasswordService;
    }

    @Path(RESET_PASSWORD_RESOURCE)
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response resetForgottenPassword(JsonNode payload) {
        return resetPasswordService.updatePassword(payload)
                .map(errors -> Response.status(BAD_REQUEST).build())
                .orElseGet(() -> Response.status(NO_CONTENT).build());
    }
}
