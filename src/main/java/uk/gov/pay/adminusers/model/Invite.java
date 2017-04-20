package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class Invite {

    private String email;
    private List<Link> links = new ArrayList<>();

    public Invite(String email, String code) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }


    @JsonProperty("_links")
    public List<Link> getLinks() {
        return links;
    }
}
