package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import io.dropwizard.jersey.PATCH;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.exception.ServiceNotFoundException;
import uk.gov.pay.adminusers.model.GovUkPayAgreement;
import uk.gov.pay.adminusers.model.SearchServicesResponse;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceSearchRequest;
import uk.gov.pay.adminusers.model.ServiceUpdateRequest;
import uk.gov.pay.adminusers.model.StripeAgreement;
import uk.gov.pay.adminusers.model.StripeAgreementRequest;
import uk.gov.pay.adminusers.model.UpdateMerchantDetailsRequest;
import uk.gov.pay.adminusers.model.User;
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
import uk.gov.service.payments.commons.api.exception.ValidationException;
import uk.gov.service.payments.commons.model.SupportedLanguage;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toUnmodifiableList;
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

@Path(SERVICES_RESOURCE)
public class ServiceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceResource.class);
    /* default */static final String HEADER_USER_CONTEXT = "GovUkPay-User-Context";
    public static final String SERVICES_RESOURCE = "/v1/api/services";

    public static final String FIELD_NAME = "name";

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
    @Operation(
            summary = "Get all services",
            tags = "Services",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Service.class))))
            }
    )
    public Response getServices() {
        LOGGER.info("Get Services request");
        List<Service> services = serviceDao.listAll().stream().map(ServiceEntity::toService).map(linksBuilder::decorate).collect(toUnmodifiableList());
        return Response
                .status(OK)
                .entity(services)
                .build();
    }

    @GET
    @Path("/{serviceExternalId}")
    @Produces(APPLICATION_JSON)
    @Operation(
            tags = "Services",
            summary = "Find service by external ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = Service.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public Response findService(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3") @PathParam("serviceExternalId")
                                String serviceExternalId) {
        return serviceDao.findByExternalId(serviceExternalId)
                .map(serviceEntity ->
                        Response.status(OK).entity(linksBuilder.decorate(serviceEntity.toService())).build())
                .orElseGet(() ->
                        Response.status(NOT_FOUND).build());
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Operation(
            tags = "Services",
            summary = "Find service associated with gateway account ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = Service.class))),
                    @ApiResponse(responseCode = "400", description = "Missing gateway account ID"),
                    @ApiResponse(responseCode = "404", description = "Not found"),
            }
    )
    public Response findServices(@Parameter(example = "1") @QueryParam("gatewayAccountId") String gatewayAccountId) {
        return serviceRequestValidator.validateFindRequest(gatewayAccountId)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> serviceServicesFactory.serviceFinder().byGatewayAccountId(gatewayAccountId)
                        .map(service -> Response.status(OK).entity(service).build())
                        .orElseGet(() -> Response.status(NOT_FOUND).build()));
    }

    @POST
    @Path("/search")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @Operation(
            tags = "Services",
            summary = "Search services by name or merchant name",
            description = "This endpoint returns a list of services using lexical meaning to determine a match to the search criteria",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(example = "{" +
                            "    \"service_name\": \"service name\"," +
                            "    \"service_merchant_name\": \"service merchant name\"" +
                            "}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = SearchServicesResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid JSON payload")
            }
    )
    public Response searchServices(JsonNode payload) {
        LOGGER.info("Search services request = [ {} ]", payload);
        var searchRequest = ServiceSearchRequest.from(payload);
        return serviceRequestValidator.validateSearchRequest(searchRequest)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> Response
                        .status(OK)
                        .entity(serviceServicesFactory.serviceFinder().bySearchRequest(searchRequest))
                        .build()
                );
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @Operation(
            tags = "Services",
            summary = "Create new service",
            description = "This endpoint creates a new service. And assigns to gateway account ids (Optional). <br> `service_name` keys are supported ISO-639-1 language codes and values are translated service names | key must be `\"en\"` or `\"cy\"`",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(example = "{" +
                            "    \"gateway_account_ids\": [\"1\"]," +
                            "    \"service_name\": {" +
                            "      \"en\": \"Some service name\"," +
                            "      \"cy\": \"Service name in welsh\"" +
                            "    }" +
                            "}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created",
                            content = @Content(schema = @Schema(implementation = Service.class))),
                    @ApiResponse(responseCode = "409", description = "Gateway account IDs provided has already been assigned to another service"),
                    @ApiResponse(responseCode = "500", description = "Invalid JSON payload")
            }
    )
    public Response createService(JsonNode payload) {
        LOGGER.info("Create Service POST request - [ {} ]", payload);
        List<String> gatewayAccountIds = extractGatewayAccountIds(payload);
        Map<SupportedLanguage, String> serviceNameVariants = getServiceNameVariants(payload);

        Service service = serviceServicesFactory.serviceCreator().doCreate(gatewayAccountIds, serviceNameVariants);
        return Response.status(CREATED).entity(service).build();

    }

    private List<String> extractGatewayAccountIds(JsonNode payload) {
        List<String> gatewayAccountIds = new ArrayList<>();
        if (payload != null && payload.get(FIELD_GATEWAY_ACCOUNT_IDS) != null) {
            payload.get(FIELD_GATEWAY_ACCOUNT_IDS)
                    .elements().forEachRemaining((node) -> gatewayAccountIds.add(node.textValue()));
        }
        return List.copyOf(gatewayAccountIds);
    }

    @Path("/{serviceExternalId}")
    @PATCH
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @Operation(
            tags = "Services",
            summary = "Patch service attributes",
            description = "Allows patching below service attributes. Each attribute has its own validation depending on data type." +
                    " Request can either be a single object or an array of objects. It’s similar to (but not 100% compliant with) [JSON Patch](http://jsonpatch.com/)." +
                    "<br>\n " +
                    " " +
                    "| op |  field | example |\n" +
                    "| --- |  --- | ----|\n" +
                    "| add | gateway_account_ids  | [\"1\"] |\n" +
                    "| replace | redirect_to_service_immediately_on_terminal_state | false |\n" +
                    "| replace | experimental_features_enabled | false |\n" +
                    "| replace | takes_payments_over_phone | false |\n" +
                    "| replace | agent_initiated_moto_enabled | false |\n" +
                    "| replace | collect_billing_address | true |\n" +
                    "| replace | current_go_live_stage | NOT_STARTED |\n" +
                    "| replace | current_psp_test_account_stage | NOT_STARTED |\n" +
                    "| replace | merchant_details/name, organisatio | name |\n" +
                    "| replace | merchant_details/address_line1, Address lin | 1 |\n" +
                    "| replace | merchant_details/address_line2, Address lin | 2  |\n" +
                    "| replace | merchant_details/address_city | London |\n" +
                    "| replace | merchant_details/address_country | GB |\n" +
                    "| replace | merchant_details/address_postcode | E6 8XX |\n" +
                    "| replace | merchant_detail |/email,  |\n" +
                    "| replace | merchant_details/email, email@exampl |.com |\n" +
                    "| replace | merchant_detail |/url,  |\n" +
                    "| replace | merchant_details/url, http://www.exampl |.org |\n" +
                    "| replace | merchant_detail |/telephone_number,  |\n" +
                    "| replace | merchant_details/telephone_number | 447700900000 |\n" +
                    "| replace | custom_branding | { \"css_url\": \"css url\", \"image_url\": \"image url\"} |\n" +
                    "| replace | custom_branding | {} |\n" +
                    "| replace | service_name/en | Some service name |\n" +
                    "| replace | sector | local government |\n" +
                    "| replace | internal | true |\n" +
                    "| replace | archived | true |\n" +
                    "| replace | went_live_date | 2022-04-09T18:07:46Z |\n" +
                    "| replace | default_billing_address_country | GB | ",
            requestBody = @RequestBody(content = @Content(array = @ArraySchema(schema = @Schema(implementation = ServiceUpdateRequest.class)))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = Service.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload")
            }
    )
    public Response updateServiceAttribute(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                           @PathParam("serviceExternalId") String serviceExternalId,
                                           JsonNode payload) {
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
    @Operation(
            tags = "Services",
            summary = "Update merchant details of a service",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = Service.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
            }
    )
    public Response updateServiceMerchantDetails(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                                 @PathParam("serviceExternalId") String serviceExternalId,
                                                 @Parameter(schema = @Schema(implementation = UpdateMerchantDetailsRequest.class))
                                                 JsonNode payload)
            throws ValidationException, ServiceNotFoundException {
        serviceRequestValidator.validateUpdateMerchantDetailsRequest(payload);
        Service service = serviceServicesFactory.serviceUpdater().doUpdateMerchantDetails(
                serviceExternalId, UpdateMerchantDetailsRequest.from(payload));
        return Response.status(OK).entity(service).build();
    }

    @Path("/{serviceExternalId}/users")
    @GET
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @Operation(
            tags = "Services",
            summary = "Find users of a service",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = User.class)))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public Response findUsersByServiceId(@PathParam("serviceExternalId") String serviceExternalId) {
        return serviceDao.findByExternalId(serviceExternalId)
                .map(serviceEntity ->
                        Response.status(200).entity(
                                userDao.findByServiceId(serviceEntity.getId())
                                        .stream()
                                        .map(UserEntity::toUser)
                                        .map(linksBuilder::decorate)
                                        .collect(toUnmodifiableList())
                        ).build()
                ).orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    // To consider for all the operations add @HeaderParam("GovUkPay-User-Context") and creating a filter
    // so we could map permissions with Regex URLs and Http method passed on to this filter.
    @Path("/{serviceExternalId}/users/{userExternalId}")
    @DELETE
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @Operation(
            tags = "Services",
            summary = "Delete user from a service",
            responses = {
                    @ApiResponse(responseCode = "204", description = "OK"),
                    @ApiResponse(responseCode = "403", description = "Forbidden. `GovUkPay-User-Context` header is blank"),
                    @ApiResponse(responseCode = "409", description = "Conflict. `GovUkPay-User-Context` is same as userExternalId or user with `userExternalId` is not admin of the service"),
            }
    )
    public Response removeUserFromService(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                          @PathParam("serviceExternalId") String serviceExternalId,
                                          @Parameter(example = "0ddf69c1ba924deca07f0ee748ff1533", description = "Admin user external ID of the service")
                                          @PathParam("userExternalId") String userExternalId,
                                          @Parameter(example = "d012mkldfdfnsdhqha7f0ee748ff1546", required = true, description = "User external ID to remove from service")
                                          @HeaderParam(HEADER_USER_CONTEXT) String userContext) {
        if (isBlank(userContext)) {
            return Response.status(Status.FORBIDDEN).build();
        } else if (userExternalId.equals(userContext)) {
            return Response.status(CONFLICT).build();
        }
        serviceServicesFactory.serviceUserRemover().remove(userExternalId, userContext, serviceExternalId);
        return Response.status(NO_CONTENT).build();
    }

    @Path("/{serviceExternalId}/stripe-agreement")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(
            tags = "Services",
            summary = "Record acceptance of Stripe terms",
            description = "Records that a GOV.UK Pay agreement has been accepted for the service.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Stripe agreement already created"),
                    @ApiResponse(responseCode = "201", description = "Created"),
                    @ApiResponse(responseCode = "422", description = "Invalid JSON payload or IP address")
            }
    )
    public Response createStripeAgreement(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                          @PathParam("serviceExternalId") String serviceExternalId,
                                          @NotNull @Valid StripeAgreementRequest stripeAgreementRequest) throws UnknownHostException {
        LOGGER.info("Create stripe agreement POST request - [ {} ]", stripeAgreementRequest.toString());

        Optional<StripeAgreement> maybeStripeAgreement = stripeAgreementService.findStripeAgreementByServiceId(serviceExternalId);
        if (maybeStripeAgreement.isPresent()) {
            LOGGER.info("Stripe agreement information is already stored for this service");
            return Response.status(OK).build();
        }
        
        stripeAgreementService.doCreate(serviceExternalId,
                InetAddress.getByName(stripeAgreementRequest.getIpAddress()));

        return Response.status(CREATED).build();
    }

    @Path("/{serviceExternalId}/stripe-agreement")
    @GET
    @Produces(APPLICATION_JSON)
    @Operation(
            tags = "Services",
            summary = "Get details about the acceptance of Stripe terms",
            description = "Retrieves the IP address and timestamp that the Stripe terms were accepted on for the service.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = StripeAgreement.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public StripeAgreement getStripeAgreement(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                              @PathParam("serviceExternalId") String serviceExternalId) {

        return stripeAgreementService.findStripeAgreementByServiceId(serviceExternalId)
                .orElseThrow(() -> new WebApplicationException(NOT_FOUND));
    }

    @Path("/{serviceExternalId}/govuk-pay-agreement")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(
            tags = "Services",
            summary = "Record acceptance of GOV.UK Pay terms",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(requiredProperties = "user_external_id",
                            example = "{" +
                                    "    \"user_external_id\": \"12e3eccfab284ae5bc1108e9c0456ba7\"" +
                                    "}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created",
                            content = @Content(schema = @Schema(implementation = GovUkPayAgreement.class))),
                    @ApiResponse(responseCode = "200", description = "Agreement already created - existing agreement returned",
                            content = @Content(schema = @Schema(implementation = GovUkPayAgreement.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
                    @ApiResponse(responseCode = "404", description = "Service with serviceExternalId not found")
            }
    )
    public Response createGovUkPayAgreement(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                            @PathParam("serviceExternalId") String serviceExternalId, JsonNode payload) {
        return govUkPayAgreementRequestValidator.validateCreateRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> createGovUkPayAgreementFromPayload(serviceExternalId, payload));
    }

    @Path("/{serviceExternalId}/govuk-pay-agreement")
    @GET
    @Produces(APPLICATION_JSON)
    @Operation(
            tags = "Services",
            summary = "Get details about the acceptance of GOV.UK Pay terms",
            description = "Retrieves the user's email address and timestamp that the GOV.UK Pay terms were accepted on for the service.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = GovUkPayAgreement.class))),
                    @ApiResponse(responseCode = "404", description = "Service with serviceExternalId not found"),
            }
    )
    public Response getGovUkPayAgreement(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                         @PathParam("serviceExternalId") String serviceExternalId) {
        return govUkPayAgreementService.findGovUkPayAgreementByServiceId(serviceExternalId)
                .map(agreement -> Response.status(OK).entity(agreement).build())
                .orElseThrow(() -> new WebApplicationException(NOT_FOUND));
    }

    private Response createGovUkPayAgreementFromPayload(String serviceExternalId, JsonNode payload) {
        return govUkPayAgreementService.findGovUkPayAgreementByServiceId(serviceExternalId).map(govUkPayAgreement -> {
            LOGGER.info("GOV.UK Pay agreement information is already stored for this service");
            return Response.status(OK).entity(govUkPayAgreement).build();
        }).orElseGet(() -> {
            ServiceEntity serviceEntity = serviceDao.findByExternalId(serviceExternalId)
                    .orElseThrow(() -> new WebApplicationException(NOT_FOUND));
            Optional<UserEntity> userEntity = userDao.findByExternalId(payload.get("user_external_id").asText());
            if (!userEntity.isPresent()) {
                return Response.status(BAD_REQUEST).entity(Errors.from("Field [user_external_id] must be a valid user ID")).build();
            }
            if (!userEntity.get().getServicesRole(serviceExternalId).isPresent()) {
                return Response.status(BAD_REQUEST).entity(Errors.from("User does not belong to the given service")).build();
            }
            GovUkPayAgreement govUkPayAgreement = govUkPayAgreementService.doCreate(serviceEntity, userEntity.get().getEmail(), ZonedDateTime.now(ZoneOffset.UTC));

            return Response.status(CREATED).entity(govUkPayAgreement).build();
        });
    }

    @Path("/{serviceExternalId}/send-live-email")
    @POST
    @Produces(APPLICATION_JSON)
    @Operation(
            tags = "Services",
            summary = "Sends an email to the user who signed the service agreement to inform them that their service is live",
            description = "This endpoint will send an email to the user who signed the agreement with GOV.UK Pay for the service informing them that their service is now live." +
                    "The email address used is the email address of the user provided to the [POST /v1/api/services/`{serviceExternalId}`/govuk-pay-agreement] endpoint.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Service with serviceExternalId not found"),
            }
    )
    public Response sendLiveAccountCreatedEmail(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                                @PathParam("serviceExternalId") String serviceExternalId) {
        serviceDao.findByExternalId(serviceExternalId)
                .orElseThrow(() -> new WebApplicationException(NOT_FOUND));
        sendLiveAccountCreatedEmailService.sendEmail(serviceExternalId);
        return Response.status(OK).build();
    }

    private Map<SupportedLanguage, String> getServiceNameVariants(JsonNode payload) {
        if (payload.hasNonNull("service_name")) {
            JsonNode serviceName = payload.get("service_name");
            return Stream.of(SupportedLanguage.values())
                    .filter(supportedLanguage -> serviceName.hasNonNull(supportedLanguage.toString()))
                    .collect(Collectors.toUnmodifiableMap(
                            supportedLanguage -> supportedLanguage,
                            supportedLanguage -> serviceName.get(supportedLanguage.toString()).asText()));
        }
        return Collections.emptyMap();
    }
}
