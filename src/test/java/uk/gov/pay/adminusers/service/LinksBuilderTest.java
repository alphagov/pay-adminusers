package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Link;
import uk.gov.pay.adminusers.model.User;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class LinksBuilderTest {

    @Test
    public void shouldConstruct_userSelfLinkCorrectly() throws Exception {

        User user = User.from("a-username", "a-password", "email@example.com", "1", "4wrwef", "123435");
        LinksBuilder linksBuilder = new LinksBuilder("http://localhost:8080");
        Link link = linksBuilder.buildSelf(user);

        String linkJson = new ObjectMapper().writeValueAsString(link);
        assertThat(linkJson, is("{\"rel\":\"self\",\"method\":\"GET\",\"href\":\"http://localhost:8080/v1/api/users/a-username\"}"));
    }
}
