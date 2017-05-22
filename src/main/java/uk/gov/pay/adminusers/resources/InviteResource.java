package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.InviteOtpRequest;
import uk.gov.pay.adminusers.model.InviteValidateOtpRequest;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.service.InviteService;
import uk.gov.pay.adminusers.service.ValidateOtpAndCreateUserResult;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Path("/v1/api/invites")
public class InviteResource {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(InviteResource.class);
    private static final int MAX_LENGTH_CODE = 255;

    private final InviteService inviteService;
    private final InviteRequestValidator inviteValidator;

    @Inject
    public InviteResource(InviteService service, InviteRequestValidator inviteValidator) {
        inviteService = service;
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

    private Response handleValidateOtpAndCreateUserException(WebApplicationException error) {
        throw error;
    }
}
