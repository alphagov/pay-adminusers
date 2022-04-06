package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.service.payments.commons.api.json.ApiResponseDateTimeSerializer;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ForgottenPassword {

    @JsonIgnore
    private Integer id;
    private String code;
    private ZonedDateTime date;
    private String userExternalId;
    private List<Link> links;

    public static ForgottenPassword forgottenPassword(String code, String userExternalId) {
        return forgottenPassword(randomInt(), code, ZonedDateTime.now(ZoneId.of("UTC")), userExternalId);
    }

    public static ForgottenPassword forgottenPassword(Integer id, String code, ZonedDateTime date, String userExternalId) {
        return new ForgottenPassword(id, code, date, userExternalId);
    }

    private ForgottenPassword(Integer id, String code, ZonedDateTime date, String userExternalId) {
        this.id = id;
        this.code = code;
        this.date = date;
        this.userExternalId = userExternalId;
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

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    @JsonProperty("_links")
    public List<Link> getLinks() {
        return links;
    }
}
