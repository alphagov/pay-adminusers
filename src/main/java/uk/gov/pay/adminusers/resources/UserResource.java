package uk.gov.pay.adminusers.resources;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.service.UserServices;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Path("/")
public class UserResource {

    private static final Logger logger = PayLoggerFactory.getLogger(UserResource.class);

    public static final String API_VERSION_PATH = "/v1";
    public static final String USERS_RESOURCE = API_VERSION_PATH + "/api/users";
    public static final String AUTHENTICATE_RESOURCE = USERS_RESOURCE + "/authenticate";
    public static final String USER_RESOURCE = USERS_RESOURCE + "/{username}";
    public static final String ATTEMPT_LOGIN_RESOURCE = USER_RESOURCE + "/attempt-login";

    private final UserServices userServices;
    private final UserRequestValidator validator;

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
        return userServices.findUser(username)
            .map(user -> Response.status(OK).type(APPLICATION_JSON).entity(user).build())
            .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    @Path(USERS_RESOURCE)
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response createUser(JsonNode node) {
        logger.info("User create request - [ {} ]", node);
        return validator.validateCreateRequest(node)
            .map(errors -> Response.status(400).entity(errors).build())
            .orElseGet(() -> {
                String roleName = node.get(User.FIELD_ROLE_NAME).asText();
                User newUser = userServices.createUser(User.from(node), roleName);
                logger.info("User created: [ {} ]", newUser);

                return Response.status(CREATED).type(APPLICATION_JSON)
                        .entity(newUser).build();
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

    private Map<String, List<String>> unauthorisedErrorMessage() {
        return ImmutableMap.of("errors", ImmutableList.of("invalid username and/or password"));
    }

    @Path(ATTEMPT_LOGIN_RESOURCE)
    @POST
    public Response updateLoginAttempts(@PathParam("username") String username, @QueryParam("action") String action) {
        logger.info("User login attempt request");
        if (isBlank(username)) {
            return Response.status(NOT_FOUND).build();
        }

        Optional<User> userOptional;

        if (!isBlank(action) && "reset".equals(action)) {
            userOptional = userServices.resetLoginAttempts(username);
        } else {
            userOptional = userServices.recordLoginAttempt(username);
        }

        return userOptional
                .map(user -> Response.status(OK).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

}
