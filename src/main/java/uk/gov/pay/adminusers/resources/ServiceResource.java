package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import io.dropwizard.jersey.PATCH;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.exception.ServiceNotFoundException;
import uk.gov.pay.adminusers.exception.ValidationException;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceUpdateRequest;
import uk.gov.pay.adminusers.model.UpdateMerchantDetailsRequest;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.service.LinksBuilder;
import uk.gov.pay.adminusers.service.ServiceServicesFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status;
import static javax.ws.rs.core.Response.Status.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.pay.adminusers.resources.ServiceRequestValidator.FIELD_GATEWAY_ACCOUNT_IDS;
import static uk.gov.pay.adminusers.resources.ServiceRequestValidator.FIELD_SERVICE_NAME;
import static uk.gov.pay.adminusers.resources.ServiceResource.SERVICES_RESOURCE;

@Path(SERVICES_RESOURCE)
public class ServiceResource {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(ServiceResource.class);
    static final String HEADER_USER_CONTEXT = "GovUkPay-User-Context";
    public static final String SERVICES_RESOURCE = "/v1/api/services";

    private final UserDao userDao;
    private final ServiceDao serviceDao;
    private final LinksBuilder linksBuilder;
    private final ServiceRequestValidator serviceRequestValidator;
    private final ServiceServicesFactory serviceServicesFactory;

    @Inject
    public ServiceResource(UserDao userDao,
                           ServiceDao serviceDao,
                           LinksBuilder linksBuilder,
                           ServiceRequestValidator serviceRequestValidator,
                           ServiceServicesFactory serviceServicesFactory
    ) {
        this.userDao = userDao;
        this.serviceDao = serviceDao;
        this.linksBuilder = linksBuilder;
        this.serviceRequestValidator = serviceRequestValidator;
        this.serviceServicesFactory = serviceServicesFactory;
    }

    @GET
    @Produces(APPLICATION_JSON)
    public Response findServices(@QueryParam("gatewayAccountId") String gatewayAccountId) {
        if (gatewayAccountId != null) {
            LOGGER.info("Find service by gateway account id request - [ {} ]", gatewayAccountId);
            return getServiceByGatewayAccountId(gatewayAccountId);
        } else {
            return getAllServices();
        }
    }

    @GET
    @Path("/{serviceExternalId}")
    @Produces(APPLICATION_JSON)
    public Response findService(@PathParam("serviceExternalId") String serviceExternalId) {
        LOGGER.info("Find Service request - [ {} ]", serviceExternalId);
        return serviceDao.findByExternalId(serviceExternalId)
                .map(serviceEntity ->
                        Response.status(OK).entity(linksBuilder.decorate(serviceEntity.toService())).build())
                .orElseGet(() ->
                        Response.status(NOT_FOUND).build());
    }


    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response createService(JsonNode payload) {
        LOGGER.info("Create Service POST request - [ {} ]", payload);
        return serviceRequestValidator.validateCreateRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
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
        if (payload == null || payload.get(FIELD_SERVICE_NAME) == null || isBlank(payload.get(FIELD_SERVICE_NAME).textValue())) {
            return Optional.empty();
        }
        return Optional.of(payload.get(FIELD_SERVICE_NAME).textValue());
    }

    @Path("/{serviceExternalId}")
    @PATCH
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response updateServiceAttribute(@PathParam("serviceExternalId") String serviceExternalId, JsonNode payload) {
        LOGGER.info("Service PATCH request - [ {} ]", serviceExternalId);
        return serviceRequestValidator.validateUpdateAttributeRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> serviceServicesFactory.serviceUpdater().doUpdate(serviceExternalId, ServiceUpdateRequest.from(payload))
                        .map(service -> Response.status(OK).entity(service).build())
                        .orElseGet(() -> Response.status(NOT_FOUND).build()));

    }

    @Path("/{serviceExternalId}/merchant-details")
    @PUT
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response updateServiceMerchantDetails(@PathParam("serviceExternalId") String serviceExternalId, JsonNode payload)
            throws ValidationException, ServiceNotFoundException {
        LOGGER.info("Service PUT request to update merchant details - [ {} ]", serviceExternalId);
        serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload);
        Service service = serviceServicesFactory.serviceUpdater().doUpdateMerchantDetails(
                serviceExternalId, UpdateMerchantDetailsRequest.from(payload));
        return Response.status(OK).entity(service).build();
    }

    @Path("/{serviceExternalId}/users")
    @GET
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response findUsersByServiceId(@PathParam("serviceExternalId") String serviceExternalId) {
        LOGGER.info("Service users GET request - [ {} ]", serviceExternalId);
        Optional<ServiceEntity> serviceEntityOptional;
        if (StringUtils.isNumeric(serviceExternalId)) {
            serviceEntityOptional = serviceDao.findById(Integer.valueOf(serviceExternalId));
        } else {
            serviceEntityOptional = serviceDao.findByExternalId(serviceExternalId);
        }

        return serviceEntityOptional.map(serviceEntity ->
                Response.status(200).entity(userDao.findByServiceId(serviceEntity.getId()).stream()
                        .map((userEntity) -> linksBuilder.decorate(userEntity.toUser()))
                        .collect(Collectors.toList())).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    // To consider for all the operations add @HeaderParam("GovUkPay-User-Context") and creating a filter

    // so we could map permissions with Regex URLs and Http method passed on to this filter.
    @Path("/{serviceExternalId}/users/{userExternalId}")
    @DELETE
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response removeUserFromService(@PathParam("serviceExternalId") String serviceId,
                                          @PathParam("userExternalId") String userId,
                                          @HeaderParam(HEADER_USER_CONTEXT) String userContext) {
        LOGGER.info("Service users DELETE request - serviceId={}, userId={}", serviceId, userId);
        if (isBlank(userContext)) {
            return Response.status(Status.FORBIDDEN).build();
        } else if (userId.equals(userContext)) {
            LOGGER.info("Failed Service users DELETE request. User and Remover cannot be the same - " +
                    "serviceId={}, removerId={}, userId={}", serviceId, userContext, userId);
            return Response.status(CONFLICT).build();
        }
        serviceServicesFactory.serviceUserRemover().remove(userId, userContext, serviceId);
        LOGGER.info("Succeeded Service users DELETE request - serviceId={}, removerId={}, userId={}", serviceId, userContext, userId);
        return Response.status(NO_CONTENT).build();
    }

    private Response getAllServices() {
        return Response.status(OK).entity(serviceDao.getAllServices()
                .stream()
                .map(serviceEntity -> linksBuilder.decorate(serviceEntity.toService()))
                .collect(Collectors.toList())).build();
    }

    private Response getServiceByGatewayAccountId(String gatewayAccountId) {
        return serviceRequestValidator.validateFindRequest(gatewayAccountId)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> serviceServicesFactory.serviceFinder().byGatewayAccountId(gatewayAccountId)
                        .map(service -> Response.status(OK).entity(service).build())
                        .orElseGet(() -> Response.status(NOT_FOUND).build()));
    }
}
