package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDateTime;

public class StripeAgreement {
    
    public static final String FIELD_IP_ADDRESS = "ip_address";
    
    @JsonIgnore
    private int serviceId;
    
    @JsonProperty(FIELD_IP_ADDRESS)
    private String ipAddress;
    
    @JsonProperty("agreement_time")
    private LocalDateTime agreementTime;

    public StripeAgreement(int serviceId, String ipAddress, LocalDateTime agreementTime) {
        this.serviceId = serviceId;
        this.ipAddress = ipAddress;
        this.agreementTime = agreementTime;
    }
    
    public int getServiceId() {
        return serviceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getAgreementTime() {
        return agreementTime;
    }
}
