package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

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

        if (rel != link.rel) return false;
        if (!method.equals(link.method)) return false;
        if (!href.equals(link.href)) return false;
        if (title != null ? !title.equals(link.title) : link.title != null) return false;
        if (type != null ? !type.equals(link.type) : link.type != null) return false;
        return params != null ? params.equals(link.params) : link.params == null;
    }

    @Override
    public int hashCode() {
        int result = rel.hashCode();
        result = 31 * result + method.hashCode();
        result = 31 * result + href.hashCode();
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (params != null ? params.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Link{" +
                "rel=" + rel +
                ", method='" + method + '\'' +
                ", href='" + href + '\'' +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", params=" + params +
                '}';
    }
}
