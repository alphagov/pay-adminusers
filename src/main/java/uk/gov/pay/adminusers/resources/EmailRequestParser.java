package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class EmailRequestParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailRequestParser.class);

    private ObjectMapper mapper;

    @Inject
    public EmailRequestParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /* default */ EmailRequest parse(JsonNode payload) throws InvalidEmailRequestException {
        try {
            String emailAddress = payload.get("address").asText();
            String gatewayAccountId = payload.get("gateway_account_external_id").asText();
            EmailTemplate template = EmailTemplate.fromString(payload.get("template").asText());
            Map<String, String> personalisation = mapper.convertValue(payload.get("personalisation"), Map.class);
            return new EmailRequest(emailAddress, gatewayAccountId, template, personalisation);
        } catch (Exception exc) {
            LOGGER.error("Error while parsing email request, exception: {}", exc.getMessage());
            throw new InvalidEmailRequestException("Error while parsing email request body", exc);
        }
    }

}
