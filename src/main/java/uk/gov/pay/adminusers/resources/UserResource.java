package uk.gov.pay.adminusers.resources;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.service.UserServices;
import uk.gov.pay.adminusers.utils.Errors;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;

@Path("/")
public class UserResource {

    private static final Logger logger = PayLoggerFactory.getLogger(UserResource.class);

    public static final String API_VERSION_PATH = "/v1";
    public static final String USERS_RESOURCE = API_VERSION_PATH + "/api/users";
    public static final String USER_RESOURCE = USERS_RESOURCE + "/{username}";

    private final UserServices userServices;
    private final UserRequestValidator validator;
    private final String baseUrl;

    @Inject
    public UserResource(String baseUrl, UserServices userServices, UserRequestValidator validator) {
        this.baseUrl = baseUrl;
        this.userServices = userServices;
        this.validator = validator;
    }

    @Path(USER_RESOURCE)
    @GET
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response getUser(@PathParam("username") String username) {
        logger.info("User GET request - [ {} ]", username);
        Optional<User> userOptional = userServices.findUser(username);
        return userOptional
                .map(user -> Response.status(OK).type(APPLICATION_JSON).entity(user).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    @Path(USERS_RESOURCE)
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response createUser(JsonNode node) {
        logger.info("User create request - [ {} ]", node);
        Optional<Errors> validationsErrors = validator.validateCreateRequest(node);

        return validationsErrors
                .map(errors -> Response.status(400).entity(errors).build())
                .orElseGet(() -> {
                    String roleName = node.get("roleName").asText();
                    User newUser = userServices.createUser(User.from(node), roleName);
                    logger.info("User created: [ {} ]", newUser);

                    return Response.status(CREATED).type(APPLICATION_JSON)
                            .entity(newUser).build();
                });
    }

}
