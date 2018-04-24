package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;

import java.util.Map;

public class EmailRequestParser {
    private static final Logger LOGGER = PayLoggerFactory.getLogger(EmailRequestParser.class);

    private static ObjectMapper mapper = new ObjectMapper();

    public static EmailRequest parse(JsonNode payload) throws InvalidEmailRequestException {
        try {
            String emailAddress = payload.get("address").asText();
            String gatewayAccountId = payload.get("gateway_account_id").asText();
            EmailTemplate template = EmailTemplate.fromString(payload.get("template").asText());
            Map<String, String> personalisation = mapper.convertValue(payload.get("personalisation"), Map.class);
            return new EmailRequest(emailAddress, gatewayAccountId, template, personalisation);
        } catch (Exception exc) {
            LOGGER.error("Error while parsing email request, exception: {}", exc.getMessage());
            throw new InvalidEmailRequestException("Error while parsing email request body");
        }
    }
}
