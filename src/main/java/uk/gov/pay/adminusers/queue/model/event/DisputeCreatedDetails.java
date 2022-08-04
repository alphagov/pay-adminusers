package uk.gov.pay.adminusers.queue.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.service.payments.commons.api.json.ApiResponseDateTimeDeserializer;

import java.time.ZonedDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DisputeCreatedDetails {

    private Long amount;
    @JsonDeserialize(using = ApiResponseDateTimeDeserializer.class)
    private ZonedDateTime evidenceDueDate;
    private String gatewayAccountId;
    private String reason;

    public DisputeCreatedDetails() {
        // empty constructor
    }

    public Long getAmount() {
        return amount;
    }

    public ZonedDateTime getEvidenceDueDate() {
        return evidenceDueDate;
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }

    public String getReason() {
        return reason;
    }
}
