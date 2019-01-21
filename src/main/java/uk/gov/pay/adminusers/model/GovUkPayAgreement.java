package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.ZonedDateTime;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class GovUkPayAgreement {

    private String email;

    private ZonedDateTime agreementTime;
    
    public GovUkPayAgreement(String email, ZonedDateTime agreementTime) {
        this.email = email;
        this.agreementTime = agreementTime;
    }
    
    public String getEmail() {
        return email;
    }

    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    public ZonedDateTime getAgreementTime() {
        return agreementTime;
    }
}
