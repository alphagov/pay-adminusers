package uk.gov.pay.adminusers.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.service.ServiceUserRemover;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
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
    public Response removeUserFromService(@PathParam("serviceExternalId") String serviceExternalId,
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
