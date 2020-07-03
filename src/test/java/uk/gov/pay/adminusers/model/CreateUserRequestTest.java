package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

public class CreateUserRequestTest {

    @Test
    public void shouldConstructAUser_fromMinimalValidUserJson() throws Exception {
        String minimumUserJson = "{" +
                "\"username\": \"a-username\"," +
                "\"telephone_number\": \"2123524\"," +
                "\"gateway_account_ids\": [\"1\", \"2\"]," +
                "\"email\": \"email@example.com\"" +
                "}";

        JsonNode jsonNode = new ObjectMapper().readTree(minimumUserJson);
        CreateUserRequest createUserRequest = CreateUserRequest.from(jsonNode);

        assertThat(createUserRequest.getUsername(), is("a-username"));
        assertThat(createUserRequest.getPassword(), notNullValue());
        assertThat(createUserRequest.getOtpKey(), notNullValue());
        assertThat(createUserRequest.getGatewayAccountIds().size(), is(2));
        assertThat(createUserRequest.getGatewayAccountIds().get(0), is("1"));
        assertThat(createUserRequest.getGatewayAccountIds().get(1), is("2"));
        assertThat(createUserRequest.getTelephoneNumber(), is("2123524"));
        assertThat(createUserRequest.getEmail(), is("email@example.com"));
    }

    @Test
    public void shouldConstructAUser_fromCompleteValidUserJson() throws Exception {
        String minimunUserJson = "{" +
                "\"username\": \"a-username\"," +
                "\"password\": \"a-password\"," +
                "\"telephone_number\": \"2123524\"," +
                "\"gateway_account_ids\": [\"1\", \"2\"]," +
                "\"otp_key\": \"fr6ysdf\"," +
                "\"email\": \"email@example.com\"" +
                "}";

        JsonNode jsonNode = new ObjectMapper().readTree(minimunUserJson);
        CreateUserRequest createUserRequest = CreateUserRequest.from(jsonNode);

        assertThat(createUserRequest.getUsername(), is("a-username"));
        assertThat(createUserRequest.getPassword(), is("a-password"));
        assertThat(createUserRequest.getGatewayAccountIds().size(), is(2));
        assertThat(createUserRequest.getGatewayAccountIds().get(0), is("1"));
        assertThat(createUserRequest.getGatewayAccountIds().get(1), is("2"));
        assertThat(createUserRequest.getTelephoneNumber(), is("2123524"));
        assertThat(createUserRequest.getOtpKey(), is("fr6ysdf"));
        assertThat(createUserRequest.getEmail(), is("email@example.com"));
    }
}
