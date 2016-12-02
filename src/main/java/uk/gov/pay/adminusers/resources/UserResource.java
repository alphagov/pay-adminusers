package uk.gov.pay.adminusers.resources;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.service.UserServices;
import uk.gov.pay.adminusers.utils.Errors;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static uk.gov.pay.adminusers.app.filters.LoggingFilter.currentRequestId;

@Path("/")
public class UserResource {

    private static final Logger logger = LoggerFactory.getLogger(UserResource.class);

    public static final String API_VERSION_PATH = "/v1";
    public static final String USERS_RESOURCE = API_VERSION_PATH + "/api/users";

    private final UserServices userServices;
    private final UserRequestValidator validator;
    private final String baseUrl;

    @Inject
    public UserResource(String baseUrl, UserServices userServices, UserRequestValidator validator) {
        this.baseUrl = baseUrl;
        this.userServices = userServices;
        this.validator = validator;
    }

    @Path(USERS_RESOURCE)
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response createUser(JsonNode node) {
        logger.info("[{}] User create request - [ {} ]", currentRequestId(), node);
        Optional<Errors> validationsErrors = validator.validateCreateRequest(node);

        return validationsErrors
                .map(errors -> Response.status(400).entity(errors).build())
                .orElseGet(() -> {
                    String roleName = node.get("roleName").asText();
                    User newUser = userServices.createUser(User.from(node), roleName);
                    logger.info("[{}] User created: [ {} ]", currentRequestId(), newUser);

                    return UserResponseBuilder.created()
                            .withUser(newUser)
                            .withBaseUrl(baseUrl).build();
                });
    }
}
