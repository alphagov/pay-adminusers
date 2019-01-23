package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import io.dropwizard.jersey.PATCH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.exception.ServiceNotFoundException;
import uk.gov.pay.adminusers.exception.ValidationException;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceUpdateRequest;
import uk.gov.pay.adminusers.model.StripeAgreement;
import uk.gov.pay.adminusers.model.StripeAgreementRequest;
import uk.gov.pay.adminusers.model.UpdateMerchantDetailsRequest;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.service.GovUkPayAgreementService;
import uk.gov.pay.adminusers.service.LinksBuilder;
import uk.gov.pay.adminusers.service.SendLiveAccountCreatedEmailService;
import uk.gov.pay.adminusers.service.ServiceServicesFactory;
import uk.gov.pay.adminusers.service.StripeAgreementService;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.commons.model.SupportedLanguage;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.pay.adminusers.resources.ServiceResource.SERVICES_RESOURCE;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_GATEWAY_ACCOUNT_IDS;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_NAME;

@Path(SERVICES_RESOURCE)
public class ServiceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceResource.class);
    static final String HEADER_USER_CONTEXT = "GovUkPay-User-Context";
    public static final String SERVICES_RESOURCE = "/v1/api/services";

    private final UserDao userDao;
    private final ServiceDao serviceDao;
    private final LinksBuilder linksBuilder;
    private final ServiceRequestValidator serviceRequestValidator;
    private final ServiceServicesFactory serviceServicesFactory;
    private final StripeAgreementService stripeAgreementService;
    private final GovUkPayAgreementRequestValidator govUkPayAgreementRequestValidator;
    private final GovUkPayAgreementService govUkPayAgreementService;
    private final SendLiveAccountCreatedEmailService sendLiveAccountCreatedEmailService;
    

    @Inject
    public ServiceResource(UserDao userDao,
                           ServiceDao serviceDao,
                           LinksBuilder linksBuilder,
                           ServiceRequestValidator serviceRequestValidator,
                           ServiceServicesFactory serviceServicesFactory,
                           StripeAgreementService stripeAgreementService,
                           GovUkPayAgreementRequestValidator govUkPayAgreementRequestValidator,
                           GovUkPayAgreementService govUkPayAgreementService,
                           SendLiveAccountCreatedEmailService sendLiveAccountCreatedEmailService) {
        this.userDao = userDao;
        this.serviceDao = serviceDao;
        this.linksBuilder = linksBuilder;
        this.serviceRequestValidator = serviceRequestValidator;
        this.serviceServicesFactory = serviceServicesFactory;
        this.stripeAgreementService = stripeAgreementService;
        this.govUkPayAgreementRequestValidator = govUkPayAgreementRequestValidator;
        this.govUkPayAgreementService = govUkPayAgreementService;
        this.sendLiveAccountCreatedEmailService = sendLiveAccountCreatedEmailService;
    }

    @GET
    @Path("/list")
    @Produces(APPLICATION_JSON)
    public Response getServices() {
        LOGGER.info("Get Services request");
        return Response
                .status(OK)
                .entity(
                        serviceDao.listAll().stream().map(
                                serviceEntity -> linksBuilder.decorate(serviceEntity.toService())
                        ).collect(Collectors.toList())
                ).build();
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

    @GET
    @Produces(APPLICATION_JSON)
    public Response findServices(@QueryParam("gatewayAccountId") String gatewayAccountId) {
        LOGGER.info("Find service by gateway account id request - [ {} ]", gatewayAccountId);
        return serviceRequestValidator.validateFindRequest(gatewayAccountId)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> serviceServicesFactory.serviceFinder().byGatewayAccountId(gatewayAccountId)
                        .map(service -> Response.status(OK).entity(service).build())
                        .orElseGet(() -> Response.status(NOT_FOUND).build()));
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response createService(JsonNode payload) {
        LOGGER.info("Create Service POST request - [ {} ]", payload);
        Optional<String> serviceName = extractServiceName(payload);
        Optional<List<String>> gatewayAccountIds = extractGatewayAccountIds(payload);
        Map<SupportedLanguage, String> serviceNameVariants = getServiceNameVariants(payload);

        Service service = serviceServicesFactory.serviceCreator().doCreate(serviceName, gatewayAccountIds, serviceNameVariants);
        return Response.status(CREATED).entity(service).build();

    }

    private Optional<List<String>> extractGatewayAccountIds(JsonNode payload) {
        if (payload == null || payload.get(FIELD_GATEWAY_ACCOUNT_IDS) == null) {
            return Optional.empty();
        }
        List<JsonNode> gatewayAccountIds = newArrayList(payload.get(FIELD_GATEWAY_ACCOUNT_IDS).elements());
        return Optional.of(gatewayAccountIds.stream()
                .map(JsonNode::textValue)
                .collect(Collectors.toList()));
    }

    private Optional<String> extractServiceName(JsonNode payload) {
        if (payload == null || payload.get(FIELD_NAME) == null || isBlank(payload.get(FIELD_NAME).textValue())) {
            return Optional.empty();
        }
        return Optional.of(payload.get(FIELD_NAME).textValue());
    }

    @Path("/{serviceExternalId}")
    @PATCH
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response updateServiceAttribute(@PathParam("serviceExternalId") String serviceExternalId, JsonNode payload) {
        LOGGER.info("Service PATCH request - [ {} ]", serviceExternalId);
        return serviceRequestValidator.validateUpdateAttributeRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> processUpdateServiceAttributePayload(serviceExternalId, payload));
    }

    private Response processUpdateServiceAttributePayload(String serviceExternalId, JsonNode payload) {

        final List<ServiceUpdateRequest> requests = ServiceUpdateRequest.getUpdateRequests(payload);
        return serviceServicesFactory.serviceUpdater().doUpdate(serviceExternalId, requests)
                .map(service -> Response.status(OK).entity(service).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
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
        serviceEntityOptional = serviceDao.findByExternalId(serviceExternalId);

        return serviceEntityOptional.map(serviceEntity ->
                Response.status(200).entity(userDao.findByServiceId(serviceEntity.getId()).stream()
                        .map(userEntity -> linksBuilder.decorate(userEntity.toUser()))
                        .collect(Collectors.toList())).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    // To consider for all the operations add @HeaderParam("GovUkPay-User-Context") and creating a filter
    // so we could map permissions with Regex URLs and Http method passed on to this filter.
    @Path("/{serviceExternalId}/users/{userExternalId}")
    @DELETE
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response removeUserFromService(@PathParam("serviceExternalId") String serviceExternalId,
                                          @PathParam("userExternalId") String userExternalId,
                                          @HeaderParam(HEADER_USER_CONTEXT) String userContext) {
        LOGGER.info("Service users DELETE request - serviceExternalId={}, userExternalId={}", serviceExternalId, userExternalId);
        if (isBlank(userContext)) {
            return Response.status(Status.FORBIDDEN).build();
        } else if (userExternalId.equals(userContext)) {
            LOGGER.info("Failed Service users DELETE request. User and Remover cannot be the same - " +
                    "serviceExternalId={}, removerExternalId={}, userExternalId={}", serviceExternalId, userContext, userExternalId);
            return Response.status(CONFLICT).build();
        }
        serviceServicesFactory.serviceUserRemover().remove(userExternalId, userContext, serviceExternalId);
        LOGGER.info("Succeeded Service users DELETE request - serviceExternalId={}, removerExternalId={}, userExternalId={}", serviceExternalId, userContext, userExternalId);
        return Response.status(NO_CONTENT).build();
    }

    @Path("/{serviceExternalId}/stripe-agreement")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response createStripeAgreement(@PathParam("serviceExternalId") String serviceExternalId,
                                          @NotNull @Valid StripeAgreementRequest stripeAgreementRequest) throws UnknownHostException {
        LOGGER.info("Create stripe agreement POST request - [ {} ]", stripeAgreementRequest.toString());

        stripeAgreementService.doCreate(serviceExternalId,
                InetAddress.getByName(stripeAgreementRequest.getIpAddress()));

        return Response.status(CREATED).build();
    }

    @Path("/{serviceExternalId}/stripe-agreement")
    @GET
    @Produces(APPLICATION_JSON)
    public StripeAgreement getStripeAgreement(@PathParam("serviceExternalId") String serviceExternalId) {

        return stripeAgreementService.findStripeAgreementByServiceId(serviceExternalId)
                .orElseThrow(() -> new WebApplicationException(NOT_FOUND));
    }
    
    @Path("/{serviceExternalId}/govuk-pay-agreement")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response createGovUkPayAgreement(@PathParam("serviceExternalId") String serviceExternalId, JsonNode payload) {
        return govUkPayAgreementRequestValidator.validateCreateRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> createGovUkPayAgreementFromPayload(serviceExternalId, payload));
    }

    @Path("/{serviceExternalId}/govuk-pay-agreement")
    @GET
    @Produces(APPLICATION_JSON)
    public Response getGovUkPayAgreement(@PathParam("serviceExternalId") String serviceExternalId) {
        return govUkPayAgreementService.findGovUkPayAgreementByServiceId(serviceExternalId)
                .map(agreement -> Response.status(OK).entity(agreement).build())
                .orElseThrow(() -> new WebApplicationException(Status.NOT_FOUND));
    }
    
    private Response createGovUkPayAgreementFromPayload(String serviceExternalId, JsonNode payload) {
        if (govUkPayAgreementService.findGovUkPayAgreementByServiceId(serviceExternalId).isPresent()) {
            return Response.status(CONFLICT)
                    .entity(Errors.from("GOV.UK Pay agreement information is already stored for this service"))
                    .build();
        }
        
        ServiceEntity serviceEntity = serviceDao.findByExternalId(serviceExternalId)
                .orElseThrow(() -> new WebApplicationException(Status.NOT_FOUND));
        Optional<UserEntity> userEntity = userDao.findByExternalId(payload.get("user_external_id").asText());
        if (!userEntity.isPresent()) {
            return Response.status(BAD_REQUEST).entity(Errors.from("Field [user_external_id] must be a valid user ID")).build();
        }
        if (!userEntity.get().getServicesRole(serviceExternalId).isPresent()) {
            return Response.status(BAD_REQUEST).entity(Errors.from("User does not belong to the given service")).build();
        }
        govUkPayAgreementService.doCreate(serviceEntity, userEntity.get().getEmail(), ZonedDateTime.now(ZoneOffset.UTC));
        
        return Response.status(CREATED).build();
    }
    
    @Path("/{serviceExternalId}/send-live-email")
    @POST
    @Produces(APPLICATION_JSON)
    public Response sendLiveAccountCreatedEmail(@PathParam("serviceExternalId") String serviceExternalId) {
        serviceDao.findByExternalId(serviceExternalId)
                .orElseThrow(() -> new WebApplicationException(NOT_FOUND));
        sendLiveAccountCreatedEmailService.sendEmail(serviceExternalId);
        return Response.status(OK).build();
    }

    private Map<SupportedLanguage, String> getServiceNameVariants(JsonNode payload) {
        if (payload.hasNonNull("service_name")) {
            JsonNode supportedLanguage = payload.get("service_name");
            Map<SupportedLanguage, String> variants = new HashMap<>();
            if (supportedLanguage.hasNonNull(SupportedLanguage.ENGLISH.toString())) {
                variants.put(SupportedLanguage.ENGLISH, supportedLanguage.get(SupportedLanguage.ENGLISH.toString()).asText());
            }
            if (supportedLanguage.hasNonNull(SupportedLanguage.WELSH.toString())) {
                variants.put(SupportedLanguage.WELSH, supportedLanguage.get(SupportedLanguage.WELSH.toString()).asText());
            }
            return variants;
        }
        return Collections.emptyMap();
    }
}
