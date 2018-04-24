package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
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

    @Test
    public void shouldCreateAnEmailRequestForAValidPayload() throws InvalidEmailRequestException {
        Map<String, Object> body = ImmutableMap.of(
          "address", "aaa@bbb.test",
          "gateway_account_id", "DIRECT_DEBIT:23847roidfghdkkj",
          "template", "MANDATE_CANCELLED",
          "personalisation", ImmutableMap.of(
                  "field 1", "theValueOfField1",
                  "field 2", "theValueOfField2"
                )
        );

        EmailRequest emailRequest = EmailRequestParser.parse(objectMapper.valueToTree(body));
        assertThat(emailRequest.getEmailAddress(), is("aaa@bbb.test"));
        assertThat(emailRequest.getGatewayAccountId(), is("DIRECT_DEBIT:23847roidfghdkkj"));
        assertThat(emailRequest.getTemplate(), is(EmailTemplate.MANDATE_CANCELLED));
        assertThat(emailRequest.getPersonalisation(), is(ImmutableMap.of(
                "field 1", "theValueOfField1",
                "field 2", "theValueOfField2"
        )));
    }

    @Test
    public void shouldThrowAnExceptionForAnInvalidPayload() throws InvalidEmailRequestException {
        Map<String, Object> body = ImmutableMap.of(
                "template", "MANDATE_CANCELLED",
                "personalisation", ImmutableMap.of(
                        "field 1", "theValueOfField1",
                        "field 2", "theValueOfField2"
                )
        );
        thrown.expect(InvalidEmailRequestException.class);
        thrown.expectMessage("Error while parsing email request body");
        thrown.reportMissingExceptionWithMessage("InvalidEmailRequestException expected");
        EmailRequestParser.parse(objectMapper.valueToTree(body));

    }
}
