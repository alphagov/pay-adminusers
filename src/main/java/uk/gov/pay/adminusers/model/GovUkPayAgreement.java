package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDateTime;

public class GovUkPayAgreement {

    public static final String FIELD_EMAIL = "email";
    @JsonIgnore
    private int serviceId;

    @JsonProperty(FIELD_EMAIL)
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
