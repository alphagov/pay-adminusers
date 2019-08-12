package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EmailRequestParserTest {

    private ObjectMapper objectMapper = new ObjectMapper();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private EmailRequestParser parser;

    @Before
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
    public void shouldThrowAnExceptionForAnInvalidPayload() throws InvalidEmailRequestException {
        Map<String, Object> body = Map.of(
                "template", "MANDATE_CANCELLED",
                "personalisation", Map.of(
                        "field 1", "theValueOfField1",
                        "field 2", "theValueOfField2"
                )
        );
        thrown.expect(InvalidEmailRequestException.class);
        thrown.expectMessage("Error while parsing email request body");
        thrown.reportMissingExceptionWithMessage("InvalidEmailRequestException expected");
        parser.parse(objectMapper.valueToTree(body));
    }
}
