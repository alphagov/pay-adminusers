package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.model.User;

import java.time.ZonedDateTime;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class LinksBuilderTest {

    private LinksBuilder linksBuilder = new LinksBuilder("http://localhost:8080");

    @Test
    public void shouldConstruct_userSelfLinkCorrectly() throws Exception {
        User user = User.from(randomInt(), randomUuid(), "a-username", "a-password", "email@example.com", Arrays.asList("1"), Arrays.asList("2"), "4wrwef", "123435");
        User decoratedUser = linksBuilder.decorate(user);

        String linkJson = new ObjectMapper().writeValueAsString(decoratedUser.getLinks().get(0));
        assertThat(linkJson, is("{\"rel\":\"self\",\"method\":\"GET\",\"href\":\"http://localhost:8080/v1/api/users/" + decoratedUser.getExternalId() + "\"}"));
    }

    @Test
    public void shouldConstruct_forgottenPasswordSelfLinkCorrectly() throws Exception {
        ForgottenPassword forgottenPassword = ForgottenPassword.forgottenPassword(1, "a-code", "a-name", ZonedDateTime.now(), "7d19aff33f8948deb97ed16b2912dcd3");
        ForgottenPassword decorated = linksBuilder.decorate(forgottenPassword);

        String linkJson = new ObjectMapper().writeValueAsString(decorated.getLinks().get(0));
        assertThat(linkJson, is("{\"rel\":\"self\",\"method\":\"GET\",\"href\":\"http://localhost:8080/v1/api/forgotten-passwords/a-code\"}"));
    }
}
