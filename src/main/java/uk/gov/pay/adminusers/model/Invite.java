package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Invite {

    private final String email;
    private List<Link> links = new ArrayList<>();

    public Invite(String email, String targetUrl) {
        this.email = email;
        Link inviteLink = Link.from(Link.Rel.invite, "GET", targetUrl);
        this.setLinks(ImmutableList.of(inviteLink));
    }

    public Invite(String email) {
        this.email = email;
    }

    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    @JsonProperty("_links")
    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }
}
