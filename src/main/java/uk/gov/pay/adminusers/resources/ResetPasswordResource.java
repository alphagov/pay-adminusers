package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.service.ResetPasswordService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static uk.gov.pay.adminusers.utils.Errors.from;

@Path("/")
public class ResetPasswordResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResetPasswordResource.class);
    private static final String RESET_PASSWORD_RESOURCE = "/v1/api/reset-password";
    /* default */ static final String FIELD_CODE = "forgotten_password_code";
    /* default */ static final String FIELD_PASSWORD = "new_password";

    private final ResetPasswordValidator resetPasswordValidator;
    private final ResetPasswordService resetPasswordService;

    @Inject
    public ResetPasswordResource(ResetPasswordValidator resetPasswordValidator, ResetPasswordService resetPasswordService) {
        this.resetPasswordValidator = resetPasswordValidator;
        this.resetPasswordService = resetPasswordService;
    }

    @Path(RESET_PASSWORD_RESOURCE)
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response resetForgottenPassword(JsonNode payload) {

        return resetPasswordValidator.validateResetRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST)
                        .type(APPLICATION_JSON)
                        .entity(errors).build())
                .orElseGet(() -> resetPasswordService.updatePassword(payload.get(FIELD_CODE).asText(), payload.get(FIELD_PASSWORD).asText())
                        .map(userId -> {
                            LOGGER.info("user ID {} updated password successfully", userId);
                            return Response.status(NO_CONTENT).build();
                        })
                        .orElseGet(() -> Response.status(NOT_FOUND)
                                .entity(from(List.of(String.format("Field [%s] non-existent/expired", FIELD_CODE))))
                                .build()));
    }
}
