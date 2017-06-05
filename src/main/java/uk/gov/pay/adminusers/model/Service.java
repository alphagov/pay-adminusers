package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Service {

    public static String DEFAULT_NAME_VALUE = "System Generated";

    public static final String FIELD_SERVICE_NAME = "name";

    private Integer id;
    private String externalId;
    private String name = DEFAULT_NAME_VALUE;
    private List<Link> links = new ArrayList<>();

    public static Service from() {
        return from(DEFAULT_NAME_VALUE);
    }

    public static Service from(String name) {
        return from(randomInt(), newId(), name);
    }

    public static Service from(Integer id, String externalId, String name) {
        return new Service(id, externalId, name);
    }

    private Service(@JsonProperty("id") Integer id,
                    @JsonProperty("external_id") String externalId,
                    @JsonProperty("name") String name) {
        this.id = id;
        this.externalId = externalId;
        this.name = name;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("_links")
    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }
}
