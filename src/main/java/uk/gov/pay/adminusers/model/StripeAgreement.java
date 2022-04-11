package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.service.payments.commons.api.json.ApiResponseDateTimeSerializer;

import java.net.InetAddress;
import java.time.ZonedDateTime;

public class StripeAgreement {

    public static final String FIELD_IP_ADDRESS = "ip_address";

    @JsonProperty(FIELD_IP_ADDRESS)
    @Schema(example = "0.0.0.0", implementation = String.class)
    private InetAddress ipAddress;

    @JsonProperty("agreement_time")
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    @Schema(example = "2022-04-09T16:01:56.820Z")
    private ZonedDateTime agreementTime;

    public StripeAgreement(InetAddress ipAddress, ZonedDateTime agreementTime) {
        this.ipAddress = ipAddress;
        this.agreementTime = agreementTime;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public ZonedDateTime getAgreementTime() {
        return agreementTime;
    }
}
