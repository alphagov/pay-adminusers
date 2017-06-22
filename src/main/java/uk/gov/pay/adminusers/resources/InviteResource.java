package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.*;
import uk.gov.pay.adminusers.service.InviteService;
import uk.gov.pay.adminusers.service.InviteServiceFactory;
import uk.gov.pay.adminusers.service.ValidateOtpAndCreateUserResult;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Path(InviteResource.INVITES_RESOURCE)
public class InviteResource {

    public static final String INVITES_RESOURCE = "/v1/api/invites";

    private static final Logger LOGGER = PayLoggerFactory.getLogger(InviteResource.class);
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
    public Response getInvite(@PathParam("code") String code) {

        LOGGER.info("Invite GET request for code - [ {} ]", code);

        if (isNotBlank(code) && code.length() > MAX_LENGTH_CODE) {
            return Response.status(NOT_FOUND).build();
        }

        return inviteService.findByCode(code)
                .map(invite -> Response.status(OK).type(APPLICATION_JSON).entity(invite).build())
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
    @Path("/otp/generate")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response generateOtp(JsonNode payload) {

        LOGGER.info("Invite POST request for generating otp");

        return inviteValidator.validateGenerateOtpRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> {
                    inviteService.generateOtp(InviteOtpRequest.from(payload));
                    return Response.status(OK).build();
                });
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
                    inviteService.generateOtp(InviteOtpRequest.from(payload));
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
    public Response validateOtpKeyForService(JsonNode payload){

        LOGGER.info("Invite POST request for validating otp for service create");

        return  inviteValidator.validateOtpValidationRequest(payload)
                .map(errors -> Response.status((BAD_REQUEST)).entity(errors).build())
                .orElseGet(() -> inviteService.validateOtp(InviteValidateOtpRequest.from(payload))
                            .map(error -> handleValidateOtpAndCreateUserException(error))
                            .orElseGet(() -> Response.status(OK).build()));
    }

    private Response handleValidateOtpAndCreateUserException(WebApplicationException error) {
        throw error;
    }
}
