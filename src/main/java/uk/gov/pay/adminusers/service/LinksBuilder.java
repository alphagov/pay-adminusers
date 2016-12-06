package uk.gov.pay.adminusers.service;

import com.google.common.collect.ImmutableList;
import uk.gov.pay.adminusers.model.Link;
import uk.gov.pay.adminusers.model.Link.Rel;
import uk.gov.pay.adminusers.model.User;

import java.net.URI;

import static javax.ws.rs.core.UriBuilder.fromUri;
import static uk.gov.pay.adminusers.resources.UserResource.USERS_RESOURCE;

public class LinksBuilder {

    private final String baseUrl;

    public LinksBuilder(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public User decorate(User user) {
        URI uri = fromUri(baseUrl).path(USERS_RESOURCE).path(user.getUsername())
                .build();
        Link selfLink = Link.from(Rel.self, "GET", uri.toString());
        user.setLinks(ImmutableList.of(selfLink));
        return user;
    }
}
