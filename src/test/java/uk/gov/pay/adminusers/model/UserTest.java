package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

public class UserTest {

    @Test
    public void shouldConstructAUser_fromMinimalValidUserJson() throws Exception {
        String minimumUserJson = "{" +
                "\"username\": \"a-username\"," +
                "\"telephoneNumber\": \"2123524\"," +
                "\"gatewayAccountId\": \"2\"," +
                "\"email\": \"email@example.com\"" +
                "}";

        JsonNode jsonNode = new ObjectMapper().readTree(minimumUserJson);
        User user = User.from(jsonNode);

        assertThat(user.getUsername(), is("a-username"));
        assertThat(user.getPassword(), notNullValue());
        assertThat(user.getOtpKey(), notNullValue());
        assertThat(user.getGatewayAccountId(), is("2"));
        assertThat(user.getTelephoneNumber(), is("2123524"));
        assertThat(user.getEmail(), is("email@example.com"));
    }

    @Test
    public void shouldConstructAUser_fromCompleteValidUserJson() throws Exception {
        String minimunUserJson = "{" +
                "\"username\": \"a-username\"," +
                "\"password\": \"a-password\"," +
                "\"telephoneNumber\": \"2123524\"," +
                "\"gatewayAccountId\": \"2\"," +
                "\"otpKey\": \"fr6ysdf\"," +
                "\"email\": \"email@example.com\"" +
                "}";

        JsonNode jsonNode = new ObjectMapper().readTree(minimunUserJson);
        User user = User.from(jsonNode);

        assertThat(user.getUsername(), is("a-username"));
        assertThat(user.getPassword(), is("a-password"));
        assertThat(user.getGatewayAccountId(), is("2"));
        assertThat(user.getTelephoneNumber(), is("2123524"));
        assertThat(user.getOtpKey(), is("fr6ysdf"));
        assertThat(user.getEmail(), is("email@example.com"));
    }
}
