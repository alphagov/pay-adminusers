package uk.gov.pay.adminusers.resources;

import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.service.LinksBuilder;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/v1/api/services")
public class ServiceResource {

    private static final Logger logger = PayLoggerFactory.getLogger(ServiceResource.class);
    private UserDao userDao;
    private ServiceDao serviceDao;
    private LinksBuilder linksBuilder;

    @Inject
    public ServiceResource(UserDao userDao, ServiceDao serviceDao, LinksBuilder linksBuilder) {
        this.userDao = userDao;
        this.serviceDao = serviceDao;
        this.linksBuilder = linksBuilder;
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
}
