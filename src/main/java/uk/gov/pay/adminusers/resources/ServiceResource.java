package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import io.dropwizard.jersey.PATCH;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.service.LinksBuilder;
import uk.gov.pay.adminusers.utils.Errors;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Path("/v1/api/services")
public class ServiceResource {

    private static final Logger logger = PayLoggerFactory.getLogger(ServiceResource.class);
    static final String FIELD_NEW_SERVICE_NAME = "new_service_name";
    private UserDao userDao;
    private ServiceDao serviceDao;
    private LinksBuilder linksBuilder;
    private ServiceRequestValidator serviceValidator;

    @Inject
    public ServiceResource(UserDao userDao, ServiceDao serviceDao, LinksBuilder linksBuilder, ServiceRequestValidator serviceValidator) {
        this.userDao = userDao;
        this.serviceDao = serviceDao;
        this.linksBuilder = linksBuilder;
        this.serviceValidator = serviceValidator;
    }

    @Path("/{serviceId}/users")
    @GET
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response findUsersByServiceId(@PathParam("serviceId") Integer serviceId) {
        logger.info("Service users GET request - [ {} ]", serviceId);
        return serviceDao.findById(serviceId).map(serviceEntity ->
                Response.status(200).entity(userDao.findByServiceId(serviceId).stream()
                        .map((userEntity) -> linksBuilder.decorate(userEntity.toUser()))
                        .collect(Collectors.toList())).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @Path("/{serviceId}")
    @PUT
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response updateServiceName(@PathParam("serviceId") Integer serviceId, JsonNode payload) {
        logger.info("Service name PUT request - [ {} ]", serviceId);
        return serviceValidator.validateUpdateRequest(payload)
                .map((Errors errors) -> Response.status(BAD_REQUEST)
                        .type(APPLICATION_JSON)
                        .entity(errors).build())
                .orElseGet(() -> {
                Optional<ServiceEntity> optionalServiceEntity = serviceDao.updateServiceName(serviceId, payload.get(FIELD_NEW_SERVICE_NAME).asText());
                //TODO return payload as well
                return Response.status(optionalServiceEntity.isPresent() ? Response.Status.OK : Response.Status.BAD_REQUEST).build();});
    }
}
