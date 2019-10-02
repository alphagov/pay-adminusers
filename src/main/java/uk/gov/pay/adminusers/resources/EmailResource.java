package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.service.EmailService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/")
public class EmailResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailResource.class);

    private final EmailService notificationService;
    private final EmailRequestParser emailRequestParser;

    @Inject
    public EmailResource(EmailService notificationService, EmailRequestParser emailRequestParser) {
        this.notificationService = notificationService;
        this.emailRequestParser = emailRequestParser;
    }

    @Path("/v1/emails/send")
    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response sendEmail(JsonNode payload) throws InvalidEmailRequestException, InvalidMerchantDetailsException {
        LOGGER.info("Received email request");
        EmailRequest emailRequest = emailRequestParser.parse(payload);
        EmailTemplate template = emailRequest.getTemplate();
        String gatewayAccountId = emailRequest.getGatewayAccountId();
        LOGGER.info("Sending {} email for account {}", template, gatewayAccountId);
        notificationService.sendEmail(
                emailRequest.getEmailAddress(),
                gatewayAccountId,
                template,
                emailRequest.getPersonalisation());
        return Response.status(Response.Status.OK).build();
    }
}
