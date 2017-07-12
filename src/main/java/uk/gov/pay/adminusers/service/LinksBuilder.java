package uk.gov.pay.adminusers.service;

import com.google.common.collect.ImmutableList;
import uk.gov.pay.adminusers.model.*;
import uk.gov.pay.adminusers.model.Link.Rel;

import java.net.URI;

import static javax.ws.rs.core.UriBuilder.fromUri;
import static uk.gov.pay.adminusers.resources.ForgottenPasswordResource.FORGOTTEN_PASSWORDS_RESOURCE;
import static uk.gov.pay.adminusers.resources.InviteResource.INVITES_RESOURCE;
import static uk.gov.pay.adminusers.resources.ServiceResource.SERVICES_RESOURCE;
import static uk.gov.pay.adminusers.resources.UserResource.USERS_RESOURCE;

public class LinksBuilder {

    private final String baseUrl;

    public LinksBuilder(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public User decorate(User user) {
        URI uri = fromUri(baseUrl).path(USERS_RESOURCE).path(user.getExternalId())
                .build();
        Link selfLink = Link.from(Rel.self, "GET", uri.toString());
        user.setLinks(ImmutableList.of(selfLink));
        return user;
    }

    public Service decorate(Service service) {
        //TODO: fix to use the externalId, after getting rid of the primary key based GET
        URI uri = fromUri(baseUrl).path(SERVICES_RESOURCE).path(String.valueOf(service.getId()))
                .build();
        Link selfLink = Link.from(Rel.self, "GET", uri.toString());
        service.setLinks(ImmutableList.of(selfLink));
        return service;
    }

    public ForgottenPassword decorate(ForgottenPassword forgottenPassword) {
        URI uri = fromUri(baseUrl).path(FORGOTTEN_PASSWORDS_RESOURCE).path(forgottenPassword.getCode())
                .build();
        Link selfLink = Link.from(Rel.self, "GET", uri.toString());
        forgottenPassword.setLinks(ImmutableList.of(selfLink));
        return forgottenPassword;
    }

    public Invite decorate(Invite invite) {
        URI uri = fromUri(baseUrl).path(INVITES_RESOURCE).path(invite.getCode())
                .build();
        Link selfLink = Link.from(Rel.self, "GET", uri.toString());
        invite.getLinks().add(selfLink);
        return invite;
    }

    public Invite addUserLink(User user, Invite invite) {
        URI uri = fromUri(baseUrl).path(USERS_RESOURCE).path(user.getExternalId())
                .build();
        Link userLink = Link.from(Rel.user, "GET", uri.toString());
        invite.getLinks().add(userLink);
        return invite;
    }
}
