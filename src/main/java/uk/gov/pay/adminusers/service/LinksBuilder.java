package uk.gov.pay.adminusers.service;

import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.Link;
import uk.gov.pay.adminusers.model.Link.Rel;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;

import java.net.URI;
import java.util.List;

import static jakarta.ws.rs.core.UriBuilder.fromUri;
import static uk.gov.pay.adminusers.resources.ForgottenPasswordResource.FORGOTTEN_PASSWORDS_RESOURCE;
import static uk.gov.pay.adminusers.resources.UserResource.USERS_RESOURCE;

public class LinksBuilder {

    private final String baseUrl;

    public LinksBuilder(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public User decorate(User user) {
        URI uri = fromUri(baseUrl).path(USERS_RESOURCE).path(user.getExternalId())
                .build();
        Link selfLink = Link.from(Rel.SELF, "GET", uri.toString());
        user.setLinks(List.of(selfLink));
        return user;
    }

    public Service decorate(Service service) {
        URI uri = fromUri(baseUrl).path("/v1/api/services").path(String.valueOf(service.getExternalId()))
                .build();
        Link selfLink = Link.from(Rel.SELF, "GET", uri.toString());
        service.setLinks(List.of(selfLink));
        return service;
    }

    public ForgottenPassword decorate(ForgottenPassword forgottenPassword) {
        URI uri = fromUri(baseUrl).path(FORGOTTEN_PASSWORDS_RESOURCE).path(forgottenPassword.getCode())
                .build();
        Link selfLink = Link.from(Rel.SELF, "GET", uri.toString());
        forgottenPassword.setLinks(List.of(selfLink));
        return forgottenPassword;
    }

    public Invite decorate(Invite invite) {
        URI uri = fromUri(baseUrl).path("/v1/api/invites").path(invite.getCode())
                .build();
        Link selfLink = Link.from(Rel.SELF, "GET", uri.toString());
        invite.getLinks().add(selfLink);
        return invite;
    }

    public Invite addUserLink(User user, Invite invite) {
        URI uri = fromUri(baseUrl).path(USERS_RESOURCE).path(user.getExternalId())
                .build();
        Link userLink = Link.from(Rel.USER, "GET", uri.toString());
        invite.getLinks().add(userLink);
        return invite;
    }
}
