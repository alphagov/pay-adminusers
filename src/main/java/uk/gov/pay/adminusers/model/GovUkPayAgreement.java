package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.service.payments.commons.api.json.ApiResponseDateTimeSerializer;

import java.time.ZonedDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class GovUkPayAgreement {

    @Schema(example = "someone@somedepartment.gov.uk")
    private String email;

    @Schema(example = "2022-04-10T16:23:35.684Z")
    private ZonedDateTime agreementTime;

    public GovUkPayAgreement(String email, ZonedDateTime agreementTime) {
        this.email = email;
        this.agreementTime = agreementTime;
    }

    public String getEmail() {
        return email;
    }

    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    public ZonedDateTime getAgreementTime() {
        return agreementTime;
    }
}
