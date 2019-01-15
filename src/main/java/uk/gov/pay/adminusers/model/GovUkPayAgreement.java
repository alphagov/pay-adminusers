package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDateTime;

public class GovUkPayAgreement {

    @JsonIgnore
    private int serviceId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("agreement_time")
    private LocalDateTime agreementTime;
    
    public GovUkPayAgreement(Integer serviceId, String email, LocalDateTime agreementTime) {
        this.serviceId = serviceId;
        this.email = email;
        this.agreementTime = agreementTime;
    }

    public int getServiceId() {
        return serviceId;
    }

    public String getEmail() {
        return email;
    }

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getAgreementTime() {
        return agreementTime;
    }
}
