package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.service.EmailService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/")
public class EmailResource {

    private static final Logger logger = PayLoggerFactory.getLogger(EmailResource.class);

    private final EmailService notificationService;

    @Inject
    public EmailResource(EmailService notificationService) {
        this.notificationService = notificationService;
    }

    @Path("/v1/emails/send")
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response sendEmail(JsonNode payload) throws InvalidEmailRequestException, InvalidMerchantDetailsException {
        logger.info("Received email request");
        EmailRequest emailRequest = EmailRequestParser.parse(payload);
        notificationService.sendEmail(
                emailRequest.getEmailAddress(),
                emailRequest.getGatewayAccountId(),
                emailRequest.getTemplate(),
                emailRequest.getPersonalisation());
        return Response.status(Response.Status.OK).build();
    }
}
