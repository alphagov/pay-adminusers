package uk.gov.pay.adminusers.resources;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.dropwizard.jersey.PATCH;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.PatchRequest;
import uk.gov.pay.adminusers.model.SecondFactorToken;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.service.UserServices;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingUsername;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.internalServerError;

@Path("/")
public class UserResource {

    private static final Logger logger = PayLoggerFactory.getLogger(UserResource.class);

    public static final String API_VERSION_PATH = "/v1";
    public static final String USERS_RESOURCE = API_VERSION_PATH + "/api/users";
    public static final String AUTHENTICATE_RESOURCE = USERS_RESOURCE + "/authenticate";
    public static final String USER_RESOURCE = USERS_RESOURCE + "/{username}";
    public static final String SECOND_FACTOR_RESOURCE = USER_RESOURCE + "/authenticate";
    public static final String SECOND_FACTOR_AUTHENTICATE_RESOURCE = SECOND_FACTOR_RESOURCE + "/{passcode}";
    public static final String ATTEMPT_LOGIN_RESOURCE = USER_RESOURCE + "/attempt-login";

    public static final String CONSTRAINT_VIOLATION_MESSAGE = "ERROR: duplicate key value violates unique constraint";


    private final UserServices userServices;
    private final UserRequestValidator validator;
    private static final int MAX_LENGTH = 255;

    @Inject
    public UserResource(UserServices userServices, UserRequestValidator validator) {
        this.userServices = userServices;
        this.validator = validator;
    }

    @Path(USER_RESOURCE)
    @GET
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response getUser(@PathParam("username") String username) {
        logger.info("User GET request - [ {} ]", username);

        if (isNotBlank(username) && username.length() > MAX_LENGTH) {
            return Response.status(NOT_FOUND).build();
        }

        return userServices.findUser(username)
                .map(user -> Response.status(OK).type(APPLICATION_JSON).entity(user).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    @Path(USERS_RESOURCE)
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response createUser(JsonNode node) {
        logger.info("Attempting user create request");
        return validator.validateCreateRequest(node)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> {
                    String roleName = node.get(User.FIELD_ROLE_NAME).asText();
                    String userName = node.get(User.FIELD_USERNAME).asText();
                    try {
                        User newUser = userServices.createUser(User.from(node), roleName);
                        logger.info("User created successfully [{}] for gateway account [{}]", newUser.getUsername(), newUser.getGatewayAccountId());
                        return Response.status(CREATED).type(APPLICATION_JSON)
                                .entity(newUser).build();
                    } catch (Exception e) {
                        return handleCreateUserException(userName, e);
                    }
                });
    }

    @Path(AUTHENTICATE_RESOURCE)
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response authenticate(JsonNode node) {
        logger.info("User authenticate request");
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

    @Path(SECOND_FACTOR_RESOURCE)
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response newSecondFactorPasscode(@PathParam("username") String username) {
        logger.info("User 2FA new passcode request");
        return userServices.newSecondFactorPasscode(username)
                //selfservice doesn't need to know the 2fa, so not sending.
                .map(twoFAToken -> Response.status(OK).type(APPLICATION_JSON).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }


    @Path(SECOND_FACTOR_AUTHENTICATE_RESOURCE)
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response authenticateSecondFactor(@PathParam("username") String username, JsonNode payload) {
        logger.info("User 2FA authenticate passcode request");
        return validator.validate2FAAuthRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> userServices.authenticateSecondFactor(username, payload.get("code").asInt())
                        .map(user -> Response.status(OK).type(APPLICATION_JSON).entity(user).build())
                        .orElseGet(() -> Response.status(UNAUTHORIZED).build()));

    }

    @Path(ATTEMPT_LOGIN_RESOURCE)
    @Produces(APPLICATION_JSON)
    @POST
    public Response updateLoginAttempts(@PathParam("username") String username, @QueryParam("action") String resetAction) {
        logger.info("User login attempt request");
        if (isBlank(username)) {
            return Response.status(NOT_FOUND).build();
        }
        if (isNotBlank(resetAction) && !resetAction.equals("reset")) {
            return Response.status(BAD_REQUEST)
                    .entity(ImmutableMap.of("errors", ImmutableList.of("Parameter [action] value is invalid"))).build();
        }

        Optional<User> userOptional;

        if (isBlank(resetAction)) {
            userOptional = userServices.recordLoginAttempt(username);
        } else {
            userOptional = userServices.resetLoginAttempts(username);
        }

        return userOptional
                .map(user -> {
                    if (user.isDisabled()) {
                        logger.warn("user {} attempted a 2fa login/reset, but account currently locked", username);
                        return Response.status(UNAUTHORIZED)
                                .entity(ImmutableMap.of("errors", ImmutableList.of(format("user [%s] locked due to too many login attempts", username))))
                                .build();
                    }
                    return Response.status(OK).entity(user).build();

                })
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    @PATCH
    @Path(USER_RESOURCE)
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response updateUserAttribute(@PathParam("username") String username, JsonNode node) {
        logger.info("User update attribute attempt request");
        return validator.validatePatchRequest(node)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> userServices.patchUser(username, PatchRequest.from(node))
                        .map(user -> Response.status(OK).entity(user).build())
                        .orElseGet(() -> Response.status(NOT_FOUND).build()));
    }

    private Response handleCreateUserException(String userName, Exception e) {
        if (e.getMessage().contains(CONSTRAINT_VIOLATION_MESSAGE)) {
            throw conflictingUsername(userName);
        } else if (e instanceof WebApplicationException) {
            throw (WebApplicationException) e;
        } else {
            logger.error("unknown database error during user creation for user [{}]", userName, e);
            throw internalServerError("unable to create user at this moment");
        }
    }

    private Map<String, List<String>> unauthorisedErrorMessage() {
        return ImmutableMap.of("errors", ImmutableList.of("invalid username and/or password"));
    }
}
