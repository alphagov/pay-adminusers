package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.service.InviteService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;

@Path("/v1/api/services/{serviceId}/invites")
public class InviteResource {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(InviteResource.class);

    private final InviteService inviteService;
    private InviteRequestValidator validator;

    @Inject
    public InviteResource(InviteService service, InviteRequestValidator validator) {
        inviteService = service;
        this.validator = validator;
    }

    @POST
    @Consumes(APPLICATION_JSON)
    public Response createInvite(@PathParam("serviceId") Integer serviceId, JsonNode payload) {

        LOGGER.info("Invite CREATE request for service - [ {} ]", serviceId);

        return validator.validateCreateRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> inviteService.createInvite(serviceId, payload.get("role_name").asText(), payload.get("email").asText())
                        .map(invite -> Response.status(ACCEPTED).build())
                        .orElseGet(() -> Response.status(NOT_FOUND).build()));
    }
}
