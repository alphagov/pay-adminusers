package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.net.InetAddress;
import java.time.ZonedDateTime;

public class StripeAgreement {
    
    public static final String FIELD_IP_ADDRESS = "ip_address";
    
    @JsonIgnore
    private int serviceId;
    
    @JsonProperty(FIELD_IP_ADDRESS)
    private InetAddress ipAddress;
    
    @JsonProperty("agreement_time")
    private ZonedDateTime agreementTime;

    public StripeAgreement(int serviceId, InetAddress ipAddress, ZonedDateTime agreementTime) {
        this.serviceId = serviceId;
        this.ipAddress = ipAddress;
        this.agreementTime = agreementTime;
    }
    
    public int getServiceId() {
        return serviceId;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    public ZonedDateTime getAgreementTime() {
        return agreementTime;
    }
}
