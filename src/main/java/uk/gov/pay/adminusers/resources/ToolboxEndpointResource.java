package uk.gov.pay.adminusers.resources;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.service.ServiceUserRemover;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import static java.lang.String.format;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.service.payments.logging.LoggingKeys.SERVICE_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.USER_EXTERNAL_ID;

@Path("/v1/api/toolbox")
public class ToolboxEndpointResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolboxEndpointResource.class);

    private final ServiceUserRemover serviceUserRemover;

    @Inject
    public ToolboxEndpointResource(ServiceUserRemover serviceUserRemover) {
        this.serviceUserRemover = serviceUserRemover;
    }

    @Path("/services/{serviceExternalId}/users/{userExternalId}")
    @DELETE
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @Operation(
            tags = "Toolbox",
            summary = "Remove user from service (Toolbox use only)",
            operationId = "removeUserFromServiceUsingToolbox",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Delete user from service"),
                    @ApiResponse(responseCode = "404", description = "User or Service not found"),
            }
    )
    public Response removeUserFromService(@Parameter(example = "7d19aff33f8948deb97ed16b2912dcd3")
                                          @PathParam("serviceExternalId") String serviceExternalId,
                                          @Parameter(example = "93ba1ec4ed6a4238a59f16ad97b4fa12")
                                          @PathParam("userExternalId") String userExternalId) {
        LOGGER.info(format("Toolbox DELETE request - removing user %s from service %s,", serviceExternalId, userExternalId),
                kv(SERVICE_EXTERNAL_ID, serviceExternalId),
                kv(USER_EXTERNAL_ID, userExternalId));
        serviceUserRemover.removeWithoutAdminCheck(userExternalId, serviceExternalId);
        LOGGER.info(format("Succeeded toolbox users DELETE request - user %s removed from service %s", serviceExternalId, userExternalId),
                kv(SERVICE_EXTERNAL_ID, serviceExternalId),
                kv(USER_EXTERNAL_ID, userExternalId));
        return Response.status(NO_CONTENT).build();
    }
}
