package uk.gov.pay.adminusers.resources.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import io.dropwizard.jersey.PATCH;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.ServiceUpdateRequest;
import uk.gov.pay.adminusers.service.ServiceServicesFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v2/api/services")
public class ServiceResourceV2 {
    private static final Logger LOGGER = PayLoggerFactory.getLogger(ServiceResourceV2.class);

    private final ServiceRequestValidatorV2 newServiceRequestValidator;
    private final ServiceServicesFactory serviceServicesFactory;

    @Inject
    public ServiceResourceV2(ServiceRequestValidatorV2 newServiceRequestValidator,
                             ServiceServicesFactory serviceServicesFactory
    ) {
        this.newServiceRequestValidator = newServiceRequestValidator;
        this.serviceServicesFactory = serviceServicesFactory;
    }

    @Path("/{serviceExternalId}")
    @PATCH
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response updateServiceAttributes(@PathParam("serviceExternalId") String serviceExternalId, JsonNode payload) {
        LOGGER.info("Service PATCH request - [ {} ]", serviceExternalId);
        return newServiceRequestValidator.validateUpdateAttributeRequest(payload)
                .map(errors -> Response.status(BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> serviceServicesFactory.serviceUpdater().doBatchUpdate(serviceExternalId, ServiceUpdateRequest.getUpdateRequests(payload))
                        .map(service -> Response.status(OK).entity(service).build())
                        .orElseGet(() -> Response.status(NOT_FOUND).build()));

    }
}
