package uk.gov.pay.adminusers.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.service.ServiceServicesFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;

@Path("/v1/api/toolbox")
public class ToolboxEndpointResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolboxEndpointResource.class);

    private final ServiceServicesFactory serviceServicesFactory;

    @Inject
    public ToolboxEndpointResource(ServiceServicesFactory serviceServicesFactory) {
        this.serviceServicesFactory = serviceServicesFactory;
    }

    @Path("/services/{serviceExternalId}/users/{userExternalId}")
    @DELETE
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response removeUserFromService(@PathParam("serviceExternalId") String serviceExternalId,
                                          @PathParam("userExternalId") String userExternalId) {
        LOGGER.info("Toolbox DELETE request - serviceExternalId={}, userExternalId={}", serviceExternalId, userExternalId);
        serviceServicesFactory.serviceUserRemover().remove(userExternalId, serviceExternalId);
        LOGGER.info("Succeeded toolbox users DELETE request - serviceExternalId={}, userExternalId={}", serviceExternalId, userExternalId);
        return Response.status(NO_CONTENT).build();
    }
}
