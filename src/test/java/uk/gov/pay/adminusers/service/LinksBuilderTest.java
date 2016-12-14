package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.model.User;

import java.time.ZonedDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class LinksBuilderTest {

    private LinksBuilder linksBuilder = new LinksBuilder("http://localhost:8080");

        @Test
    public void shouldConstruct_userSelfLinkCorrectly() throws Exception {

        User user = User.from("a-username", "a-password", "email@example.com", "1", "4wrwef", "123435");
        User decoratedUser = linksBuilder.decorate(user);

        String linkJson = new ObjectMapper().writeValueAsString(decoratedUser.getLinks().get(0));
        assertThat(linkJson, is("{\"rel\":\"self\",\"method\":\"GET\",\"href\":\"http://localhost:8080/v1/api/users/a-username\"}"));
    }

    @Test
    public void shouldConstruct_forgottenPasswordSelfLinkCorrectly() throws Exception {
        ForgottenPassword forgottenPassword = ForgottenPassword.forgottenPassword(1, "a-code", "a-name", ZonedDateTime.now());
        ForgottenPassword decorated = linksBuilder.decorate(forgottenPassword);

        String linkJson = new ObjectMapper().writeValueAsString(decorated.getLinks().get(0));
        assertThat(linkJson, is("{\"rel\":\"self\",\"method\":\"GET\",\"href\":\"http://localhost:8080/v1/api/forgotten-passwords/a-code\"}"));
    }
}
