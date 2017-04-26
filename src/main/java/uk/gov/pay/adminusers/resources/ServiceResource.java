package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.InviteRequest;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.service.InviteService;
import uk.gov.pay.adminusers.service.LinksBuilder;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;

@Path("/v1/api/services")
public class ServiceResource {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(ServiceResource.class);
    private UserDao userDao;
    private ServiceDao serviceDao;
    private final InviteService inviteService;
    private final InviteRequestValidator inviteValidator;
    private LinksBuilder linksBuilder;

    @Inject
    public ServiceResource(UserDao userDao,
                           ServiceDao serviceDao,
                           InviteService inviteService,
                           InviteRequestValidator inviteValidator,
                           AdminUsersConfig config,
                           LinksBuilder linksBuilder) {
        this.userDao = userDao;
        this.serviceDao = serviceDao;
        this.inviteService = inviteService;
        this.inviteValidator = inviteValidator;
        this.linksBuilder = linksBuilder;
    }

    @Path("/{serviceId}/users")
    @GET
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response findUsersByServiceId(@PathParam("serviceId") Integer serviceId) {
        LOGGER.info("Service users GET request - [ {} ]", serviceId);
        return serviceDao.findById(serviceId).map(serviceEntity ->
                Response.status(200).entity(userDao.findByServiceId(serviceId).stream()
                        .map((userEntity) -> linksBuilder.decorate(userEntity.toUser()))
                        .collect(Collectors.toList())).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path("/{serviceId}/invites")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response createServiceInvite(@PathParam("serviceId") Integer serviceId, JsonNode payload) {

        LOGGER.info("Invite CREATE request for service - [ {} ]", serviceId);

        return inviteValidator.validateCreateRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> inviteService.create(InviteRequest.from(payload), serviceId)
                        .map(invite -> Response.status(CREATED).entity(invite).build())
                        .orElseGet(() -> Response.status(NOT_FOUND).build()));
    }
}
