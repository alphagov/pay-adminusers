package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.service.ForgottenPasswordServices;
import uk.gov.pay.adminusers.utils.Errors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Path(ForgottenPasswordResource.FORGOTTEN_PASSWORDS_RESOURCE)
@Tag(name = "Passwords")
public class ForgottenPasswordResource {

    public static final String FORGOTTEN_PASSWORDS_RESOURCE = "/v1/api/forgotten-passwords";
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgottenPasswordResource.class);

    private static final int MAX_LENGTH = 255;
    private final ForgottenPasswordServices forgottenPasswordServices;
    private final ForgottenPasswordValidator validator;

    @Inject
    public ForgottenPasswordResource(ForgottenPasswordServices forgottenPasswordServices) {
        this.forgottenPasswordServices = forgottenPasswordServices;
        validator = new ForgottenPasswordValidator();
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @Operation(
            summary = "Create a new forgotten password request (sends email to user)",
            requestBody = @RequestBody(content = @Content(schema = @Schema(requiredProperties = {"username"},
                    example = "{" +
                            "    \"username\": \"a75f2719456c80eb29a10bbf1c92e7b7@example.com\"" +
                            "}"))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Invalid payload"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public Response sendForgottenPassword(JsonNode payload) {
        LOGGER.info("ForgottenPassword CREATE request");
        Optional<Errors> errorsOptional = validator.validateCreateRequest(payload);
        return errorsOptional
                .map(errors ->
                        Response.status(BAD_REQUEST).type(APPLICATION_JSON).entity(errors).build())
                .orElseGet(() -> {
                    forgottenPasswordServices.create(payload.get("username").asText());
                    return Response.status(OK).build();
                });
    }

    @Path("/{code}")
    @GET
    @Produces(APPLICATION_JSON)
    @Operation(
            summary = "Verify forgotten password code",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = ForgottenPassword.class))),
                    @ApiResponse(responseCode = "404", description = "Not found")
            }
    )
    public Response findNonExpiredForgottenPassword(@Parameter(example = "bc9039e00cba4e63b2c92ecd0e188aba") @PathParam("code") String code) {
        LOGGER.info("ForgottenPassword GET request");
        if (isNotBlank(code) && code.length() > MAX_LENGTH) {
            return Response.status(NOT_FOUND).build();
        }
        return forgottenPasswordServices.findNonExpired(code)
                .map(forgottenPassword -> Response.status(OK).type(APPLICATION_JSON).entity(forgottenPassword).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }
}
