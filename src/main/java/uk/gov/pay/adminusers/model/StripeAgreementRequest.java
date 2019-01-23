package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.adminusers.resources.ValidIpAddress;

import javax.validation.constraints.NotNull;

public class StripeAgreementRequest {

    @NotNull
    @ValidIpAddress
    private final String ipAddress;
    
    @JsonCreator
    public StripeAgreementRequest(@JsonProperty("ip_address") String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public String toString() {
        return "StripeAgreementRequest{" +
                "ipAddress='" + ipAddress + '\'' +
                '}';
    }
}
