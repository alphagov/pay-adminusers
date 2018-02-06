package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.*;
import uk.gov.pay.adminusers.service.*;
import uk.gov.pay.adminusers.utils.Errors;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.internalServerError;

@Path(InviteResource.INVITES_RESOURCE)
public class InviteResource {

    public static final String INVITES_RESOURCE = "/v1/api/invites";

    private static final Logger LOGGER = PayLoggerFactory.getLogger(InviteResource.class);
    private static final int MAX_LENGTH_CODE = 255;

    private final InviteService inviteService;
    private final InviteRequestValidator inviteValidator;
    private final InviteServiceFactory inviteServiceFactory;
    private final ServiceRequestValidator serviceRequestValidator;

    @Inject
    public InviteResource(InviteService service, InviteRequestValidator inviteValidator, InviteServiceFactory inviteServiceFactory, ServiceRequestValidator serviceRequestValidator) {
        inviteService = service;
        this.inviteServiceFactory = inviteServiceFactory;
        this.inviteValidator = inviteValidator;
        this.serviceRequestValidator = serviceRequestValidator;
    }

    @GET
    @Path("/{code}")
    @Produces(APPLICATION_JSON)
    public Response getInvite(@PathParam("code") String code) {

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
    public Response completeInvite(@PathParam("code") String inviteCode, JsonNode payload) {
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
            List<String> gatewayAccountIds = newArrayList(payload.get(InviteCompleteRequest.FIELD_GATEWAY_ACCOUNT_IDS).elements())
                    .stream().map(gatewayAccountIdNode -> gatewayAccountIdNode.textValue()).collect(Collectors.toList());
            inviteCompleteRequest.setGatewayAccountIds(gatewayAccountIds);
        }
        return inviteCompleteRequest;
    }

    @POST
    @Path("{code}/otp/generate")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response generateAndDispatchOtp(@PathParam("code") String inviteCode, JsonNode payload) {
        LOGGER.info("Invite POST request for generating otp");
        if (isNotBlank(inviteCode) && inviteCode.length() > MAX_LENGTH_CODE) {
            return Response.status(NOT_FOUND).build();
        }

        return inviteServiceFactory.inviteOtpRouter().routeOtpDispatch(inviteCode)
                .map(inviteOtpDispatcherValidate -> {
                    InviteOtpDispatcher otpDispatcher = inviteOtpDispatcherValidate.getLeft();
                    if(inviteOtpDispatcherValidate.getRight()){
                        Optional<Errors> errors = inviteValidator.validateGenerateOtpRequest(payload);
                        if(errors.isPresent()){
                            return Response.status(BAD_REQUEST).entity(errors).build();
                        }
                    }
                    if(otpDispatcher.withData(InviteOtpRequest.from(payload)).dispatchOtp(inviteCode)){
                        return Response.status(OK).build();
                    } else {
                        throw internalServerError("unable to dispatch otp at this moment");
                    }
                })
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    @POST
    @Path("/service")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
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
    public Response createUserInvite(JsonNode payload) {
        LOGGER.info("Initiating user invitation request");
        return inviteValidator.validateCreateUserRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> inviteServiceFactory.userInvite().doInvite(InviteUserRequest.from(payload))
                        .map(invite -> Response.status(CREATED).entity(invite).build())
                        .orElseGet(() -> Response.status(NOT_FOUND).build())
                );
    }

    @POST
    @Path("/otp/resend")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response resendOtp(JsonNode payload) {

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
    public Response createUserUponOtpValidation(JsonNode payload) {

        LOGGER.info("Invite POST request for validating otp and creating user");

        return inviteValidator.validateOtpValidationRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> {
                    ValidateOtpAndCreateUserResult validateOtpAndCreateUserResult = inviteService.validateOtpAndCreateUser(InviteValidateOtpRequest.from(payload));
                    if (!validateOtpAndCreateUserResult.isError()) {
                        User createdUser = validateOtpAndCreateUserResult.getUser();
                        LOGGER.info("User created successfully from invitation [{}] for gateway accounts [{}]", createdUser.getExternalId(), String.join(", ", createdUser.getGatewayAccountIds()));
                        return Response.status(CREATED).type(APPLICATION_JSON).entity(createdUser).build();
                    }
                    return handleValidateOtpAndCreateUserException(validateOtpAndCreateUserResult.getError());
                });
    }

    @POST
    @Path("/otp/validate/service")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response validateOtpKeyForService(JsonNode payload) {

        LOGGER.info("Invite POST request for validating otp for service create");

        return inviteValidator.validateOtpValidationRequest(payload)
                .map(errors -> Response.status((BAD_REQUEST)).entity(errors).build())
                .orElseGet(() -> inviteService.validateOtp(InviteValidateOtpRequest.from(payload))
                        .map(error -> handleValidateOtpAndCreateUserException(error))
                        .orElseGet(() -> Response.status(OK).build()));
    }

    private Response handleValidateOtpAndCreateUserException(WebApplicationException error) {
        throw error;
    }
}
