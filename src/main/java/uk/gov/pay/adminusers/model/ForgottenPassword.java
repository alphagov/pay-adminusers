package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.service.payments.commons.api.json.ApiResponseDateTimeSerializer;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ForgottenPassword {

    @JsonIgnore
    private final Integer id;
    private final String code;
    private final ZonedDateTime date;
    private final String userExternalId;
    private final String username;
    private List<Link> links;

    public static ForgottenPassword forgottenPassword(String code, String userExternalId, String username) {
        return forgottenPassword(randomInt(), code, ZonedDateTime.now(ZoneOffset.UTC), userExternalId, username);
    }

    public static ForgottenPassword forgottenPassword(Integer id, String code, ZonedDateTime date,
                                                      String userExternalId, String username) {
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

    @Schema(example = "bc9039e00cba4e63b2c92ecd0e188aba")
    public String getCode() {
        return code;
    }

    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    @Schema(example = "2022-04-06T21:27:06.376Z")
    public ZonedDateTime getDate() {
        return date;
    }

    @Schema(example = "12e3eccfab284ae5bc1108e9c0456ba7")
    public String getUserExternalId() {
        return userExternalId;
    }

    @Schema(example = "username@example.gov.uk")
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
