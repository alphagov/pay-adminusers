package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ForgottenPassword {
    @JsonIgnore
    private Integer id;
    private String code;
    private ZonedDateTime date;
    private String userExternalId;
    private String username;
    private List<Link> links;

    public static ForgottenPassword forgottenPassword(String code, String userExternalId, String username) {
        return forgottenPassword(randomInt(), code, userExternalId, username, ZonedDateTime.now(ZoneId.of("UTC")));
    }

    public static ForgottenPassword forgottenPassword(Integer id, String code, String userExternalId, String username, ZonedDateTime date) {
        return new ForgottenPassword(id, code, date, userExternalId, username);
    }

    private ForgottenPassword(Integer id, String code, ZonedDateTime date, String userExternalId, String username) {
        this.id = id;
        this.code = code;
        this.date = date;
        this.userExternalId = userExternalId;
        this.username = username;
    }

    public Integer getId() {
        return id;
    }

    public String getUserExternalId() {
        return userExternalId;
    }

    public String getCode() {
        return code;
    }

    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    public ZonedDateTime getDate() {
        return date;
    }

    public String getUsername() {
        return username;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    @JsonProperty("_links")
    public List<Link> getLinks() {
        return links;
    }
}
