package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateServiceRequest(
        List<String> gatewayAccountIds,
        @JsonDeserialize(using = ServiceNamesDeserializer.class) Map<SupportedLanguage, String> serviceName) {
}

