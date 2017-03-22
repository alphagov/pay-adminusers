package uk.gov.pay.adminusers.resources;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.dropwizard.jersey.PATCH;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.PatchRequest;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.service.UserServices;
import uk.gov.pay.adminusers.service.UserServicesFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingUsername;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.internalServerError;

@Path("/")
public class UserResource {

    private static final Logger logger = PayLoggerFactory.getLogger(UserResource.class);

    public static final String API_VERSION_PATH = "/v1";
    public static final String USERS_RESOURCE = API_VERSION_PATH + "/api/users";
    private static final String AUTHENTICATE_RESOURCE = USERS_RESOURCE + "/authenticate";
    private static final String USER_RESOURCE = USERS_RESOURCE + "/{externalId}";
    private static final String SECOND_FACTOR_RESOURCE = USER_RESOURCE + "/second-factor";
    private static final String SECOND_FACTOR_AUTHENTICATE_RESOURCE = SECOND_FACTOR_RESOURCE + "/authenticate";
    private static final String USER_SERVICES_RESOURCE = USER_RESOURCE + "/services";
    private static final String USER_SERVICE_RESOURCE = USER_SERVICES_RESOURCE + "/{service-id}";

    public static final String CONSTRAINT_VIOLATION_MESSAGE = "ERROR: duplicate key value violates unique constraint";


    private final UserServices userServices;
    private final UserServicesFactory userServicesFactory;

    private final UserRequestValidator validator;

    private static final int USER_EXTERNAL_ID_LENGTH = 32;
    private static final int USER_USERNAME_MAX_LENGTH = 255;

    private static final String IS_NEW_API_REQUEST_PARAMETER_KEY = "is_new_api_request";

    @Inject
    public UserResource(UserServices userServices, UserRequestValidator validator, UserServicesFactory userServicesFactory) {
        this.userServices = userServices;
        this.validator = validator;
        this.userServicesFactory = userServicesFactory;
    }

    @Path(USER_RESOURCE)
    @GET
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response getUser(@PathParam("externalId") String externalId, @QueryParam(IS_NEW_API_REQUEST_PARAMETER_KEY) String isNewApiRequest) {
        logger.info("User GET request - [ {} ]", externalId);
        if (isNotBlank(isNewApiRequest)) {
            if (isNotBlank(externalId) && (externalId.length() != USER_EXTERNAL_ID_LENGTH)) {
                return Response.status(NOT_FOUND).build();
            }

            return userServices.findUserByExternalId(externalId)
                    .map(user -> Response.status(OK).type(APPLICATION_JSON).entity(user).build())
                    .orElseGet(() -> Response.status(NOT_FOUND).build());
        } else {
            if (isNotBlank(externalId) && (externalId.length() > USER_USERNAME_MAX_LENGTH)) {
                return Response.status(NOT_FOUND).build();
            }

            return userServices.findUserByUsername(externalId)
                    .map(user -> Response.status(OK).type(APPLICATION_JSON).entity(user).build())
                    .orElseGet(() -> Response.status(NOT_FOUND).build());
        }
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
                        logger.info("User created successfully [{}] for gateway accounts [{}]", newUser.getExternalId(), String.join(", ", newUser.getGatewayAccountIds()));
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
    public Response newSecondFactorPasscode(@PathParam("externalId") String externalId) {
        logger.info("User 2FA new passcode request");
        return userServices.newSecondFactorPasscode(externalId)
                .map(twoFAToken -> Response.status(OK).type(APPLICATION_JSON).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }


    @Path(SECOND_FACTOR_AUTHENTICATE_RESOURCE)
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response authenticateSecondFactor(@PathParam("externalId") String externalId, JsonNode payload) {
        logger.info("User 2FA authenticate passcode request");
        return validator.validate2FAAuthRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> userServices.authenticateSecondFactor(externalId, payload.get("code").asInt())
                        .map(user -> Response.status(OK).type(APPLICATION_JSON).entity(user).build())
                        .orElseGet(() -> Response.status(UNAUTHORIZED).build()));

    }

    @PATCH
    @Path(USER_RESOURCE)
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response updateUserAttribute(@PathParam("externalId") String externalId, JsonNode node) {
        logger.info("User update attribute attempt request");
        return validator.validatePatchRequest(node)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> userServices.patchUser(externalId, PatchRequest.from(node))
                        .map(user -> Response.status(OK).entity(user).build())
                        .orElseGet(() -> Response.status(NOT_FOUND).build()));
    }

    @PUT
    @Path(USER_SERVICE_RESOURCE)
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response updateServiceRole(@PathParam("username") String username, @PathParam("service-id") Integer serviceId, JsonNode payload) {
        logger.info("User update service role request");
        return validator.validateServiceRole(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> {
                    String roleName = payload.get(User.FIELD_ROLE_NAME).asText();
                    return userServicesFactory.serviceRoleUpdater().doUpdate(username, serviceId, roleName)
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
            logger.error("unknown database error during user creation for user [{}]", userName, e);
            throw internalServerError("unable to create user at this moment");
        }
    }

    private Map<String, List<String>> unauthorisedErrorMessage() {
        return ImmutableMap.of("errors", ImmutableList.of("invalid username and/or password"));
    }
}
