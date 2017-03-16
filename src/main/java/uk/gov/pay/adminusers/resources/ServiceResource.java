package uk.gov.pay.adminusers.resources;

import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/v1/api/services")
public class ServiceResource {

    private static final Logger logger = PayLoggerFactory.getLogger(ServiceResource.class);
    private UserDao userDao;

    @Inject
    public ServiceResource(UserDao userDao) {
        this.userDao = userDao;
    }

    @Path("/{serviceId}/users")
    @GET
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response findUsersByServiceId(@PathParam("serviceId") Integer serviceId) {
        logger.info("Service users GET request - [ {} ]", serviceId);
        return Response.status(200).entity(userDao.findByServiceId(serviceId).stream().map(UserEntity::toUser).collect(Collectors.toList())).build();
    }
}
