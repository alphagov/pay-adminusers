package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.service.ForgottenPasswordServices;
import uk.gov.pay.adminusers.utils.Errors;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;

@Path("/")
public class ForgottenPasswordResource {

    public static final String FORGOTTEN_PASSWORDS_RESOURCE = "/v1/api/forgotten-passwords";
    public static final String FORGOTTEN_PASSWORD_RESOURCE = FORGOTTEN_PASSWORDS_RESOURCE + "/{code}";
    private static final Logger logger = PayLoggerFactory.getLogger(ForgottenPasswordResource.class);

    private final ForgottenPasswordServices forgottenPasswordServices;
    private final ForgottenPasswordValidator validator;

    @Inject
    public ForgottenPasswordResource(ForgottenPasswordServices forgottenPasswordServices) {
        this.forgottenPasswordServices = forgottenPasswordServices;
        validator = new ForgottenPasswordValidator();
    }

    @Path(FORGOTTEN_PASSWORDS_RESOURCE)
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response createForgottenPassword(JsonNode payload) {
        logger.info("ForgottenPassword CREATE request - [ {} ]", payload);
        Optional<Errors> errorsOptional = validator.validateCreateRequest(payload);
        return errorsOptional
                .map(errors ->
                        Response.status(BAD_REQUEST).type(APPLICATION_JSON).entity(errors).build())
                .orElseGet(() ->
                        forgottenPasswordServices.create(payload.get("username").asText())
                                .map(forgottenPassword ->
                                        Response.status(CREATED).type(APPLICATION_JSON).entity(forgottenPassword).build())
                                .orElseGet(() ->
                                        Response.status(NOT_FOUND).build()));
    }

    @Path(FORGOTTEN_PASSWORD_RESOURCE)
    @GET
    @Produces(APPLICATION_JSON)
    public Response getForgottenPassword(@PathParam("code") String code) {
        logger.info("ForgottenPassword GET request - [ {} ]", code);
        return forgottenPasswordServices.find(code)
                .map(forgottenPassword -> Response.status(OK).type(APPLICATION_JSON).entity(forgottenPassword).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }
}
