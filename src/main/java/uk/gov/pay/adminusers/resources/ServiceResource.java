package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.InviteRequest;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.service.InviteService;
import uk.gov.pay.adminusers.service.LinksBuilder;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;
import static uk.gov.pay.adminusers.model.Service.FIELD_SERVICE_NAME;

@Path("/v1/api/services")
public class ServiceResource {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(ServiceResource.class);
    private UserDao userDao;
    private ServiceDao serviceDao;
    private final InviteService inviteService;
    private final InviteRequestValidator inviteValidator;
    private LinksBuilder linksBuilder;
    private final ServiceRequestValidator serviceRequestValidator;

    @Inject
    public ServiceResource(UserDao userDao,
                           ServiceDao serviceDao,
                           InviteService inviteService,
                           InviteRequestValidator inviteValidator,
                           LinksBuilder linksBuilder,
                           ServiceRequestValidator serviceRequestValidator
    ) {
        this.userDao = userDao;
        this.serviceDao = serviceDao;
        this.inviteService = inviteService;
        this.inviteValidator = inviteValidator;
        this.linksBuilder = linksBuilder;
        this.serviceRequestValidator = serviceRequestValidator;
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response createService(JsonNode payload) {
        LOGGER.info("Create Service POST request - [ {} ]", payload);
        return serviceRequestValidator.validateCreateRequest(payload)
                .map(errors -> Response.status(Response.Status.BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> {

                    List<String> gatewayAccountIds = new ArrayList<String>(payload.get("gateway_account_ids").size());
                    for (JsonNode id : payload.get("gateway_account_ids")) {
                        gatewayAccountIds.add(id.asText());
                    }

                    ServiceEntity serviceEntity = new ServiceEntity(gatewayAccountIds);
                    serviceEntity.setName(payload.get(FIELD_SERVICE_NAME).asText());

                    serviceEntity.setExternalId(newId());
                    serviceDao.persist(serviceEntity);
                    return Response.status(Response.Status.CREATED).entity(serviceEntity.toService()).build();
                });

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
