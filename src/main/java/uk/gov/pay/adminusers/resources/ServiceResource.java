package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.InviteUserRequest;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.service.InviteService;
import uk.gov.pay.adminusers.service.LinksBuilder;
import uk.gov.pay.adminusers.service.ServiceServicesFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;
import static uk.gov.pay.adminusers.resources.ServiceRequestValidator.FIELD_GATEWAY_ACCOUNT_IDS;
import static uk.gov.pay.adminusers.resources.ServiceRequestValidator.FIELD_SERVICE_NAME;
import static uk.gov.pay.adminusers.resources.ServiceResource.SERVICES_RESOURCE;

@Path(SERVICES_RESOURCE)
public class ServiceResource {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(ServiceResource.class);
    private UserDao userDao;
    private ServiceDao serviceDao;
    private final InviteService inviteService;
    private final InviteRequestValidator inviteValidator;
    private LinksBuilder linksBuilder;
    private final ServiceRequestValidator serviceRequestValidator;
    private final ServiceServicesFactory serviceServicesFactory;

    public static final String API_VERSION_PATH = "/v1";
    public static final String SERVICES_RESOURCE = API_VERSION_PATH + "/api/services";

    @Inject
    public ServiceResource(UserDao userDao,
                           ServiceDao serviceDao,
                           InviteService inviteService,
                           InviteRequestValidator inviteValidator,
                           LinksBuilder linksBuilder,
                           ServiceRequestValidator serviceRequestValidator,
                           ServiceServicesFactory serviceServicesFactory
    ) {
        this.userDao = userDao;
        this.serviceDao = serviceDao;
        this.inviteService = inviteService;
        this.inviteValidator = inviteValidator;
        this.linksBuilder = linksBuilder;
        this.serviceRequestValidator = serviceRequestValidator;
        this.serviceServicesFactory = serviceServicesFactory;
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response createService(JsonNode payload) {
        LOGGER.info("Create Service POST request - [ {} ]", payload);
        return serviceRequestValidator.validateCreateRequest(payload)
                .map(errors -> Response.status(Response.Status.BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> {
                    Optional<String> serviceName = extractServiceName(payload);
                    Optional<List<String>> gatewayAccountIds = extractGatewayAccountIds(payload);

                    Service service = serviceServicesFactory.serviceCreator().doCreate(serviceName, gatewayAccountIds);
                    return Response.status(CREATED).entity(service).build();
                });

    }

    private Optional<List<String>> extractGatewayAccountIds(JsonNode payload) {
        if (payload == null || payload.get(FIELD_GATEWAY_ACCOUNT_IDS) == null) {
            return Optional.empty();
        }
        List<JsonNode> gatewayAccountIds = newArrayList(payload.get(FIELD_GATEWAY_ACCOUNT_IDS).elements());
        return Optional.of(gatewayAccountIds.stream()
                        .map(idNode -> idNode.textValue())
                        .collect(Collectors.toList()));
    }

    private Optional<String> extractServiceName(JsonNode payload) {
        if (payload == null || payload.get(FIELD_SERVICE_NAME) == null || StringUtils.isBlank(payload.get(FIELD_SERVICE_NAME).textValue())) {
            return Optional.empty();
        }
        return Optional.of(payload.get(FIELD_SERVICE_NAME).textValue());
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
                .orElseGet(() -> inviteService.create(InviteUserRequest.from(payload), serviceId)
                        .map(invite -> Response.status(CREATED).entity(invite).build())
                        .orElseGet(() -> Response.status(NOT_FOUND).build()));
    }
}
