package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.dropwizard.jersey.PATCH;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.CreateUserRequest;
import uk.gov.pay.adminusers.model.PatchRequest;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.service.UserServices;
import uk.gov.pay.adminusers.service.UserServicesFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;
import static uk.gov.pay.adminusers.model.User.FIELD_USERNAME;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingUsername;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.internalServerError;

@Path("/")
public class UserResource {

    private static final Logger logger = PayLoggerFactory.getLogger(UserResource.class);

    public static final String API_VERSION_PATH = "/v1";
    public static final String USERS_RESOURCE = API_VERSION_PATH + "/api/users";
    public static final String FIND_RESOURCE = USERS_RESOURCE + "/find";
    private static final String AUTHENTICATE_RESOURCE = USERS_RESOURCE + "/authenticate";
    private static final String USER_RESOURCE = USERS_RESOURCE + "/{externalId}";
    private static final String SECOND_FACTOR_RESOURCE = USER_RESOURCE + "/second-factor";
    private static final String SECOND_FACTOR_AUTHENTICATE_RESOURCE = SECOND_FACTOR_RESOURCE + "/authenticate";
    private static final String USER_SERVICES_RESOURCE = USER_RESOURCE + "/services";
    private static final String USER_SERVICE_RESOURCE = USER_SERVICES_RESOURCE + "/{serviceExternalId}";

    public static final String CONSTRAINT_VIOLATION_MESSAGE = "ERROR: duplicate key value violates unique constraint";

    private final UserServices userServices;
    private final UserServicesFactory userServicesFactory;

    private final UserRequestValidator validator;

    @Inject
    public UserResource(UserServices userServices, UserRequestValidator validator, UserServicesFactory userServicesFactory) {
        this.userServices = userServices;
        this.validator = validator;
        this.userServicesFactory = userServicesFactory;
    }


    @Path(FIND_RESOURCE)
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response findUser(JsonNode payload) {
        logger.info("User FIND request");
        return validator.validateFindRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> userServices.findUserByUsername(payload.get(FIELD_USERNAME).asText())
                        .map(user -> Response.status(OK).type(APPLICATION_JSON).entity(user).build())
                        .orElseGet(() -> Response.status(NOT_FOUND).build()));
    }

    @Path(USER_RESOURCE)
    @GET
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response getUser(@PathParam("externalId") String externalId) {
        logger.info("User GET request - [ {} ]", externalId);
        return userServices.findUserByExternalId(externalId)
                .map(user -> Response.status(OK).type(APPLICATION_JSON).entity(user).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    @Path(USERS_RESOURCE)
    @GET
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response getUsers(@QueryParam("ids") String externalIds) {
        logger.info("Users GET request - [ {} ]", externalIds);
        List<String> externalIdsList = Arrays
                .stream(externalIds.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        List<User> users = userServices.findUsersByExternalIds(externalIdsList);

        if (users.size() == externalIdsList.size()) {
            return Response.status(OK).type(APPLICATION_JSON).entity(users).build();
        } else {
            return Response.status(NOT_FOUND).build();
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
                    String roleName = node.get(CreateUserRequest.FIELD_ROLE_NAME).asText();
                    String userName = node.get(CreateUserRequest.FIELD_USERNAME).asText();
                    try {
                        User newUser = userServicesFactory.userCreator().doCreate(CreateUserRequest.from(node), roleName);
                        logger.info("User created successfully [{}]", newUser.getExternalId());
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
    public Response updateServiceRole(@PathParam("externalId") String userExternalId, @PathParam("serviceExternalId") String serviceExternalId, JsonNode payload) {
        logger.info("User update service role request");
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
    @Path(USER_SERVICES_RESOURCE)
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response createServiceRole(@PathParam("externalId") String userExternalId, JsonNode payload) {
        logger.info("Assign service role to a user {} request", userExternalId);
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
            logger.error("unknown database error during user creation for user [{}]", userName, e);
            throw internalServerError("unable to create user at this moment");
        }
    }

    private Map<String, List<String>> unauthorisedErrorMessage() {
        return ImmutableMap.of("errors", ImmutableList.of("invalid username and/or password"));
    }
}
