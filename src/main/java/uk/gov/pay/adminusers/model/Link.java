package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;
import java.util.Objects;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Link {

    public enum Rel {
        self,
        invite,
        user
    }

    private Rel rel;
    private String method;
    private String href;
    private String title;
    private String type;
    private Map<String, Object> params;

    public static Link from(Rel rel, String method, String href) {
        return new Link(rel, method, href);
    }

    private Link(@JsonProperty("rel") Rel rel,
                 @JsonProperty("method") String method,
                 @JsonProperty("href") String href) {
        this.rel = rel;
        this.method = method;
        this.href = href;
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

    public String getTitle() {
        return title;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Link link = (Link) o;

        return Objects.equals(rel, link.rel)
                && Objects.equals(method, link.method)
                && Objects.equals(href, link.href)
                && Objects.equals(title, link.title)
                && Objects.equals(type, link.type)
                && Objects.equals(params, link.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rel, method, href, title, type, params);
    }

    @Override
    public String toString() {
        return String.format("Link{rel=%s, method='%s', href='%s', title='%s', type='%s', params=%s}",
                rel, method, href, title, type, params);
    }
}
