package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.InviteCompleteRequest;
import uk.gov.pay.adminusers.model.InviteCompleteResponse;
import uk.gov.pay.adminusers.model.InviteOtpRequest;
import uk.gov.pay.adminusers.model.InviteServiceRequest;
import uk.gov.pay.adminusers.model.InviteUserRequest;
import uk.gov.pay.adminusers.model.InviteValidateOtpRequest;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.service.InviteCompleter;
import uk.gov.pay.adminusers.service.InviteOtpDispatcher;
import uk.gov.pay.adminusers.service.InviteService;
import uk.gov.pay.adminusers.service.InviteServiceFactory;
import uk.gov.pay.adminusers.service.ValidateOtpAndCreateUserResult;
import uk.gov.pay.adminusers.utils.Errors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.internalServerError;

@Path(InviteResource.INVITES_RESOURCE)
@Tag(name = "Invites")
public class InviteResource {

    public static final String INVITES_RESOURCE = "/v1/api/invites";

    private static final Logger LOGGER = LoggerFactory.getLogger(InviteResource.class);
    private static final int MAX_LENGTH_CODE = 255;

    private final InviteService inviteService;
    private final InviteRequestValidator inviteValidator;
    private final InviteServiceFactory inviteServiceFactory;

    @Inject
    public InviteResource(InviteService service, InviteRequestValidator inviteValidator, InviteServiceFactory inviteServiceFactory) {
        inviteService = service;
        this.inviteServiceFactory = inviteServiceFactory;
        this.inviteValidator = inviteValidator;
    }

    @GET
    @Path("/{code}")
    @Produces(APPLICATION_JSON)
    @Operation(
            summary = "Find invite for invite code",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = Invite.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public Response getInvite(@Parameter(example = "d02jddeib0lqpsir28fbskg9v0rv") @PathParam("code") String code) {

        LOGGER.info("Invite GET request for code - [ {} ]", code);

        if (isNotBlank(code) && code.length() > MAX_LENGTH_CODE) {
            return Response.status(NOT_FOUND).build();
        }
        return inviteServiceFactory.inviteFinder().find(code)
                .map(invite -> Response.status(OK).type(APPLICATION_JSON).entity(invite).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    @POST
    @Path("/{code}/complete")
    @Produces(APPLICATION_JSON)
    @Operation(
            summary = "Completes the invite by creating user/service and invalidating the invite code",
            description = "In the case of a user invite, this resource will assign the new service to the existing user and disables the invite. <br>" +
                    "In the case of a service invite, this resource will create a new service, assign gateway account ids (if provided) and also creates a new user and assign to the service<br>" +
                    "The response contains the user and the service id's affected as part of the invite completion in addition to the invite",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = InviteCompleteResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public Response completeInvite(@Parameter(example = "d02jddeib0lqpsir28fbskg9v0rv") @PathParam("code") String inviteCode,
                                   @Parameter(schema = @Schema(implementation = InviteCompleteRequest.class))
                                           JsonNode payload) {
        LOGGER.info("Invite  complete POST request for code - [ {} ]", inviteCode);

        if (isNotBlank(inviteCode) && inviteCode.length() > MAX_LENGTH_CODE) {
            return Response.status(NOT_FOUND).build();
        }

        return inviteServiceFactory.inviteCompleteRouter().routeComplete(inviteCode)
                .map(inviteCompleterAndValidate -> {
                    InviteCompleter inviteCompleter = inviteCompleterAndValidate.getLeft();
                    return inviteCompleter.withData(inviteCompleteRequestFrom(payload)).complete(inviteCode)
                            .map(inviteCompleteResponse -> Response.status(OK).entity(inviteCompleteResponse).build())
                            .orElseGet(() -> Response.status(NOT_FOUND).build());
                })
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    private InviteCompleteRequest inviteCompleteRequestFrom(JsonNode payload) {
        InviteCompleteRequest inviteCompleteRequest = new InviteCompleteRequest();
        if (payload != null && payload.get(InviteCompleteRequest.FIELD_GATEWAY_ACCOUNT_IDS) != null) {
            List<String> gatewayAccountIds = new ArrayList<>();
            payload.get(InviteCompleteRequest.FIELD_GATEWAY_ACCOUNT_IDS)
                    .elements()
                    .forEachRemaining((node) -> gatewayAccountIds.add(node.textValue()));
            inviteCompleteRequest.setGatewayAccountIds(gatewayAccountIds);
        }
        return inviteCompleteRequest;
    }

    @POST
    @Path("{code}/otp/generate")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(
            summary = "Generates and sends otp verification code to the phone number registered in the invite.",
            requestBody = @RequestBody(content = @Content(schema =
            @Schema(example = "{" +
                    " \"telephone_number\": \"07451234567\"," +
                    " \"password\": \"a-password\"" +
                    "}", requiredProperties = {"telephone_number", "password"}))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public Response generateAndDispatchOtp(@Parameter(example = "d02jddeib0lqpsir28fbskg9v0rv") @PathParam("code") String inviteCode,
                                           JsonNode payload) {
        LOGGER.info("Invite POST request for generating otp");
        if (isNotBlank(inviteCode) && inviteCode.length() > MAX_LENGTH_CODE) {
            return Response.status(NOT_FOUND).build();
        }

        return inviteServiceFactory.inviteOtpRouter().routeOtpDispatch(inviteCode)
                .map(inviteOtpDispatcherValidate -> {
                    if (inviteOtpDispatcherValidate.getRight()) {
                        Optional<Errors> errors = inviteValidator.validateGenerateOtpRequest(payload);
                        if (errors.isPresent()) {
                            return Response.status(BAD_REQUEST).entity(errors).build();
                        }
                    }

                    InviteOtpDispatcher otpDispatcher = inviteOtpDispatcherValidate.getLeft();
                    if(otpDispatcher.withData(InviteOtpRequest.from(payload)).dispatchOtp(inviteCode)){
                        return Response.status(OK).build();
                    } else {
                        throw internalServerError("unable to dispatch otp at this moment");
                    }
                })
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Operation(
            summary = "List invites for a service",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Invite.class))))
            }
    )
    public Response getInvites(@Parameter(example = "ahq8745yq387") @QueryParam("serviceId") String serviceId) {
        LOGGER.info("List invites GET request for service - [ {} ]", serviceId);
        List<Invite> invites = inviteServiceFactory.inviteFinder().findAllActiveInvites(serviceId);
        return Response.status(OK).type(APPLICATION_JSON).entity(invites).build();
    }

    @POST
    @Path("/service")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(
            summary = "Creates an invitation to allow self provisioning new service with Pay",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(example = "{" +
                            "\"telephone_number\":\"+440787654534\"," +
                            "\"email\": \"example@example.gov.uk\"," +
                            "\"password\" : \"plain-txt-passsword\"" +
                            "}", requiredProperties = {"telephone_number", "email", "password"}))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created",
                            content = @Content(schema = @Schema(implementation = Invite.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload")
            }
    )
    public Response createServiceInvite(JsonNode payload) {
        LOGGER.info("Initiating create service invitation request");
        return inviteValidator.validateCreateServiceRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> {
                    Invite invite = inviteServiceFactory.serviceInvite().doInvite(InviteServiceRequest.from(payload));
                    return Response.status(CREATED).entity(invite).build();
                });
    }

    @POST
    @Path("/user")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(
            summary = "Creates an invitation to allow a new team member to join an existing service.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Created",
                            content = @Content(schema = @Schema(implementation = Invite.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
                    @ApiResponse(responseCode = "404", description = "Not found"),

            }
    )
    public Response createUserInvite(@Parameter(schema = @Schema(implementation = InviteUserRequest.class))
                                             JsonNode payload) {
        LOGGER.info("Initiating user invitation request");
        return inviteValidator.validateCreateUserRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> inviteServiceFactory.userInvite().doInvite(InviteUserRequest.from(payload))
                        .map(invite -> Response.status(CREATED).entity(invite).build())
                        .orElseGet(() -> Response.status(NOT_FOUND).entity(StringUtils.EMPTY).build())
                );
    }

    @POST
    @Path("/otp/resend")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(
            summary = "Resend OTP",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Invalid payload")
            }
    )
    public Response resendOtp(@Parameter(schema = @Schema(implementation = InviteOtpRequest.class))
                                      JsonNode payload) {

        LOGGER.info("Invite POST request for resending otp");

        return inviteValidator.validateResendOtpRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> {
                    inviteService.reGenerateOtp(InviteOtpRequest.from(payload));
                    return Response.status(OK).build();
                });
    }

    @POST
    @Path("/otp/validate")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(
            summary = "Validates OTP for the invite and creates user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Invalid payload")
            }
    )
    public Response createUserUponOtpValidation(@Parameter(schema = @Schema(implementation = InviteValidateOtpRequest.class))
                                                        JsonNode payload) {

        LOGGER.info("Invite POST request for validating otp and creating user");

        return inviteValidator.validateOtpValidationRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> {
                    ValidateOtpAndCreateUserResult validateOtpAndCreateUserResult = inviteService.validateOtpAndCreateUser(InviteValidateOtpRequest.from(payload));
                    if (!validateOtpAndCreateUserResult.isError()) {
                        User createdUser = validateOtpAndCreateUserResult.getUser();
                        String serviceIds = createdUser.getServiceRoles().stream()
                                .map(serviceRole -> serviceRole.getService().getExternalId())
                                .collect(Collectors.joining(", "));
                        
                        LOGGER.info("User created successfully from invitation [{}] for services [{}]", createdUser.getExternalId(), serviceIds);
                        return Response.status(CREATED).type(APPLICATION_JSON).entity(createdUser).build();
                    }
                    return handleValidateOtpAndCreateUserException(validateOtpAndCreateUserResult.getError());
                });
    }

    @POST
    @Path("/otp/validate/service")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(
            summary = "Validates OTP for the invite - part of creating service",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Invalid payload")
            }
    )
    public Response validateOtpKeyForService(@Parameter(schema = @Schema(implementation = InviteValidateOtpRequest.class))
                                                     JsonNode payload) {

        LOGGER.info("Invite POST request for validating otp for service create");

        return inviteValidator.validateOtpValidationRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> inviteService.validateOtp(InviteValidateOtpRequest.from(payload))
                        .map(this::handleValidateOtpAndCreateUserException)
                        .orElseGet(() -> Response.status(OK).build()));
    }

    private Response handleValidateOtpAndCreateUserException(WebApplicationException error) {
        throw error;
    }
}
