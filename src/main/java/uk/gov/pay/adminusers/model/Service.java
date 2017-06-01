package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Service {

    public static String DEFAULT_NAME_VALUE = "System Generated";

    private Integer id;

    private String externalId;

    private String name = DEFAULT_NAME_VALUE;

    public static Service from(Integer id, String externalId, String name) {
        return new Service(id, externalId, name);
    }

    private Service(@JsonProperty("id") Integer id,
                    @JsonProperty("externalId") String externalId,
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
}
