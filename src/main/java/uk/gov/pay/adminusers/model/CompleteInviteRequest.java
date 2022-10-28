package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CompleteInviteRequest {

    @ArraySchema(schema = @Schema(example = "1",
            description = "gateway_account_ids that needs to be associated for the new service. Only applicable for invite type `service`"))
    private List<String> gatewayAccountIds = Collections.emptyList();
    
    public CompleteInviteRequest() {
        // for Jackson deserialisation
    }
    
    public CompleteInviteRequest(List<String> gatewayAccountIds) {
        this.gatewayAccountIds = gatewayAccountIds;
    }

    public List<String> getGatewayAccountIds() {
        return gatewayAccountIds;
    }
}
