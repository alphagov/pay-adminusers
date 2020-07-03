package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EmailRequestParserTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    private EmailRequestParser parser;

    @BeforeEach
    public void setUp() {
        parser = new EmailRequestParser(objectMapper);
    }

    @Test
    public void shouldCreateAnEmailRequestForAValidPayload() throws InvalidEmailRequestException {
        Map<String, Object> body = Map.of(
          "address", "aaa@bbb.test",
          "gateway_account_external_id", "DIRECT_DEBIT:23847roidfghdkkj",
          "template", "MANDATE_CANCELLED",
          "personalisation", Map.of(
                  "field 1", "theValueOfField1",
                  "field 2", "theValueOfField2"
                )
        );

        EmailRequest emailRequest = parser.parse(objectMapper.valueToTree(body));
        assertThat(emailRequest.getEmailAddress(), is("aaa@bbb.test"));
        assertThat(emailRequest.getGatewayAccountId(), is("DIRECT_DEBIT:23847roidfghdkkj"));
        assertThat(emailRequest.getTemplate(), is(EmailTemplate.MANDATE_CANCELLED));
        assertThat(emailRequest.getPersonalisation(), is(Map.of(
                "field 1", "theValueOfField1",
                "field 2", "theValueOfField2"
        )));
    }

    @Test
    public void shouldThrowAnExceptionForAnInvalidPayload() {
        Map<String, Object> body = Map.of(
                "template", "MANDATE_CANCELLED",
                "personalisation", Map.of(
                        "field 1", "theValueOfField1",
                        "field 2", "theValueOfField2"
                )
        );
        InvalidEmailRequestException exception = assertThrows(InvalidEmailRequestException.class,
                () -> parser.parse(objectMapper.valueToTree(body)));
        assertThat(exception.getMessage(), is("Error while parsing email request body"));
    }
}
