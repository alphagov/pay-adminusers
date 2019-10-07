package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Locale;
import java.util.Objects;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Link {

    public enum Rel {
        SELF,
        INVITE,
        USER
    }

    private final Rel rel;
    private final String method;
    private final String href;

    public static Link from(Rel rel, String method, String href) {
        return new Link(rel, method, href);
    }

    private Link(Rel rel, String method, String href) {
        this.rel = rel;
        this.method = method;
        this.href = href;
    }

    @JsonProperty("rel")
    public String getRelAsLowerCase() {
        return rel.name().toLowerCase(Locale.ENGLISH);
    }

    public Rel getRel() {
        return rel;
    }

    public String getMethod() {
        return method;
    }

    public String getHref() {
        return href;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Link link = (Link) o;

        return Objects.equals(rel, link.rel)
                && Objects.equals(method, link.method)
                && Objects.equals(href, link.href);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rel, method, href);
    }

    @Override
    public String toString() {
        return String.format("Link{rel=%s, method='%s', href='%s'}",
                getRelAsLowerCase(), method, href);
    }
}
