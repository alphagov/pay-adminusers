package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.service.EmailService;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

//TODO remove this resource as it's only applicable for sending direct debit emails
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
