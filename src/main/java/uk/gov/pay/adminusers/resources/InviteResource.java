package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.InviteOtpRequest;
import uk.gov.pay.adminusers.service.InviteService;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Path("/v1/api/invites")
public class InviteResource {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(InviteResource.class);
    private static final int MAX_LENGTH_CODE = 255;

    private final InviteService inviteService;
    private final InviteRequestValidator inviteValidator;

    @Inject
    public InviteResource(InviteService service, InviteRequestValidator inviteValidator) {
        inviteService = service;
        this.inviteValidator = inviteValidator;
    }

    @GET
    @Path("/{code}")
    @Produces(APPLICATION_JSON)
    public Response getInvite(@PathParam("code") String code) {

        LOGGER.info("Invite GET request for code - [ {} ]", code);

        if (isNotBlank(code) && code.length() > MAX_LENGTH_CODE) {
            return Response.status(NOT_FOUND).build();
        }

        return inviteService.findByCode(code)
                .map(invite -> Response.status(OK).type(APPLICATION_JSON).entity(invite).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    @POST
    @Path("/{code}/otp")
    @Consumes(APPLICATION_JSON)
    public Response generateOtp(@PathParam("code") String code, JsonNode payload) {

        LOGGER.info("Invite POST request for generating for code - [ {} ]", code);

        if (isNotBlank(code) && code.length() > MAX_LENGTH_CODE) {
            return Response.status(NOT_FOUND).build();
        }

        return inviteValidator.validateOtpRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> {
                    inviteService.generateOtp(InviteOtpRequest.from(code, payload));
                    return Response.status(OK).build();
                });
    }
}
