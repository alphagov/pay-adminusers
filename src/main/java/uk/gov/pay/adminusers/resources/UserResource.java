package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Splitter;
import com.google.inject.Inject;
import io.dropwizard.jersey.PATCH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.CreateUserRequest;
import uk.gov.pay.adminusers.model.PatchRequest;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.service.ExistingUserOtpDispatcher;
import uk.gov.pay.adminusers.service.UserServices;
import uk.gov.pay.adminusers.service.UserServicesFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static uk.gov.pay.adminusers.model.User.FIELD_USERNAME;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingUsername;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.internalServerError;

@Path(UserResource.USERS_RESOURCE)
public class UserResource {
    
    public static final String USERS_RESOURCE = "/v1/api/users";

    private static final Logger LOGGER = LoggerFactory.getLogger(UserResource.class);

    private static final Splitter COMMA_SEPARATOR = Splitter.on(',').trimResults();

    public static final String CONSTRAINT_VIOLATION_MESSAGE = "ERROR: duplicate key value violates unique constraint";

    private final UserServices userServices;
    private final UserServicesFactory userServicesFactory;

    private final ExistingUserOtpDispatcher existingUserOtpDispatcher;

    private final UserRequestValidator validator;

    @Inject
    public UserResource(UserServices userServices, UserRequestValidator validator, UserServicesFactory userServicesFactory,
                        ExistingUserOtpDispatcher existingUserOtpDispatcher) {
        this.userServices = userServices;
        this.validator = validator;
        this.userServicesFactory = userServicesFactory;
        this.existingUserOtpDispatcher = existingUserOtpDispatcher;
    }


    @Path("/find")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response findUser(JsonNode payload) {
        LOGGER.info("User FIND request");
        return validator.validateFindRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> userServices.findUserByUsername(payload.get(FIELD_USERNAME).asText())
                        .map(user -> Response.status(OK).type(APPLICATION_JSON).entity(user).build())
                        .orElseGet(() -> Response.status(NOT_FOUND).build()));
    }

    @Path("/{userExternalId}")
    @GET
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response getUser(@PathParam("userExternalId") String externalId) {
        LOGGER.info("User GET request - [ {} ]", externalId);
        return userServices.findUserByExternalId(externalId)
                .map(user -> Response.status(OK).type(APPLICATION_JSON).entity(user).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }
    
    @POST
    @Path("/admin-emails-for-gateway-accounts")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Map<String, List<String>> getAdminUserEmailsForGatewayAccountIds(@Valid Map<String, List<String>> gatewayAccountIds) {
        return userServices.getAdminUserEmailsForGatewayAccountIds(gatewayAccountIds.get("gatewayAccountIds"));
    }
    
    @GET
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response getUsers(@QueryParam("ids") String externalIds) {
        LOGGER.info("Users GET request - [ {} ]", externalIds);
        List<String> externalIdsList = COMMA_SEPARATOR.splitToList(externalIds);

        List<User> users = userServices.findUsersByExternalIds(externalIdsList);

        return Response.status(OK).type(APPLICATION_JSON).entity(users).build();
    }
    
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response createUser(JsonNode node) {
        LOGGER.info("Attempting user create request");
        return validator.validateCreateRequest(node)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> {
                    String roleName = node.get(CreateUserRequest.FIELD_ROLE_NAME).asText();
                    String userName = node.get(CreateUserRequest.FIELD_USERNAME).asText();
                    try {
                        User newUser = userServicesFactory.userCreator().doCreate(CreateUserRequest.from(node), roleName);
                        LOGGER.info("User created successfully [{}]", newUser.getExternalId());
                        return Response.status(CREATED).type(APPLICATION_JSON)
                                .entity(newUser).build();
                    } catch (Exception e) {
                        return handleCreateUserException(userName, e);
                    }
                });
    }

    @Path("/authenticate")
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response authenticate(JsonNode node) {
        LOGGER.info("User authenticate request");
        return validator.validateAuthenticateRequest(node)
                .map(errors -> Response.status(400).entity(errors).build())
                .orElseGet(() -> {
                    Optional<User> userOptional = userServices.authenticate(
                            node.get("username").asText(),
                            node.get("password").asText());

                    return userOptional
                            .map(user -> Response.status(OK).type(APPLICATION_JSON).entity(user).build())
                            .orElseGet(() ->
                                    Response.status(UNAUTHORIZED).type(APPLICATION_JSON)
                                            .entity(unauthorisedErrorMessage())
                                            .build());
                });
    }

    @Path("/{userExternalId}/second-factor")
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response sendOtpSms(@PathParam("userExternalId") String externalId, JsonNode payload) {
        LOGGER.info("User 2FA new passcode request");
        return validator.validateNewSecondFactorPasscodeRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> {
                    boolean changingSignInMethod = payload != null && payload.get("provisional") != null && payload.get("provisional").asBoolean();

                    if (changingSignInMethod) {
                        return existingUserOtpDispatcher.sendChangeSignMethodToSmsOtp(externalId)
                                .map(twoFAToken -> Response.status(OK).type(APPLICATION_JSON).build())
                                .orElseGet(() -> Response.status(NOT_FOUND).build());
                    }

                    return existingUserOtpDispatcher.sendSignInOtp(externalId)
                            .map(twoFAToken -> Response.status(OK).type(APPLICATION_JSON).build())
                            .orElseGet(() -> Response.status(NOT_FOUND).build());
                });
    }

    @Path("/{userExternalId}/second-factor/authenticate")
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response authenticateSecondFactor(@PathParam("userExternalId") String externalId, JsonNode payload) {
        LOGGER.info("User 2FA authenticate passcode request");
        return validator.validate2FAAuthRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> userServices.authenticateSecondFactor(externalId, payload.get("code").asInt())
                        .map(user -> Response.status(OK).type(APPLICATION_JSON).entity(user).build())
                        .orElseGet(() -> Response.status(UNAUTHORIZED).build()));
    }

    @Path("/{userExternalId}/second-factor/provision")
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response newSecondFactorOtpKey(@PathParam("userExternalId") String externalId) {
        LOGGER.info("User 2FA provision new OTP key request");
        return userServices.provisionNewOtpKey(externalId)
                .map(user -> Response.status(OK).type(APPLICATION_JSON).entity(user).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    @Path("/{userExternalId}/second-factor/activate")
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response activateSecondFactorOtpKey(@PathParam("userExternalId") String externalId, JsonNode payload) {
        LOGGER.info("User 2FA activate new OTP key request");
        return validator.validate2faActivateRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> {
                    int code = payload.get("code").asInt();
                    SecondFactorMethod secondFactor = SecondFactorMethod.valueOf(payload.get("second_factor").asText());
                    return userServices.activateNewOtpKey(externalId, secondFactor, code)
                            .map(user -> Response.status(OK).type(APPLICATION_JSON).entity(user).build())
                            .orElseGet(() -> Response.status(UNAUTHORIZED).build());
                });
    }
    
    @Path("/{userExternalId}/reset-second-factor")
    @POST
    @Produces(APPLICATION_JSON)
    public Response resetSecondFactor(@PathParam("userExternalId") String externalId) {
        return userServices.resetSecondFactor(externalId)
                .map(user -> Response.status(OK).entity(user).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    @PATCH
    @Path("/{userExternalId}")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response updateUserAttribute(@PathParam("userExternalId") String externalId, JsonNode node) {
        LOGGER.info("User update attribute attempt request");
        return validator.validatePatchRequest(node)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> userServices.patchUser(externalId, PatchRequest.from(node))
                        .map(user -> Response.status(OK).entity(user).build())
                        .orElseGet(() -> Response.status(NOT_FOUND).build()));
    }

    @PUT
    @Path("/{userExternalId}/services/{serviceExternalId}")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response updateServiceRole(@PathParam("userExternalId") String userExternalId, @PathParam("serviceExternalId") String serviceExternalId, JsonNode payload) {
        LOGGER.info("User update service role request");
        return validator.validateServiceRole(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> {
                    String roleName = payload.get(User.FIELD_ROLE_NAME).asText();
                    return userServicesFactory.serviceRoleUpdater().doUpdate(userExternalId, serviceExternalId, roleName)
                            .map(user -> Response.status(OK).entity(user).build())
                            .orElseGet(() -> Response.status(NOT_FOUND).build());
                });
    }

    @POST
    @Path("/{userExternalId}/services")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response createServiceRole(@PathParam("userExternalId") String userExternalId, JsonNode payload) {
        LOGGER.info("Assign service role to a user {} request", userExternalId);
        return validator.validateAssignServiceRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> {
                    String serviceExternalId = payload.get(User.FIELD_SERVICE_EXTERNAL_ID).asText();
                    String roleName = payload.get(User.FIELD_ROLE_NAME).asText();
                    return userServicesFactory.serviceRoleCreator().doCreate(userExternalId, serviceExternalId, roleName)
                            .map(user -> Response.status(OK).entity(user).build())
                            .orElseGet(() -> Response.status(NOT_FOUND).build());
                });

    }

    private Response handleCreateUserException(String userName, Exception e) {
        if (e.getMessage().contains(CONSTRAINT_VIOLATION_MESSAGE)) {
            throw conflictingUsername(userName);
        } else if (e instanceof WebApplicationException) {
            throw (WebApplicationException) e;
        } else {
            LOGGER.error("unknown database error during user creation for user [{}]", userName, e);
            throw internalServerError("unable to create user at this moment");
        }
    }

    private Map<String, List<String>> unauthorisedErrorMessage() {
        return Map.of("errors", List.of("invalid username and/or password"));
    }
}
