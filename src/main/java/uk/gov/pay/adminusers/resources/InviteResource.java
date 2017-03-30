package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.service.InviteService;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.ACCEPTED;

@Path("/v1/api/services/{serviceId}/invites")
public class InviteResource {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(InviteResource.class);

    private final InviteService inviteService;

    @Inject
    public InviteResource(InviteService service) {
        inviteService = service;
    }

    @POST
    @Consumes(APPLICATION_JSON)
    public Response createInvite(@PathParam("serviceId") Integer serviceId, JsonNode payload) {
        LOGGER.info("Invite CREATE request for service - [ {} ]", serviceId);
        inviteService.create(Invite.from(payload));
        return Response.status(ACCEPTED).build();
    }
}
