package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InviteCompleteRequest {

    public static final String FIELD_GATEWAY_ACCOUNT_IDS = "gateway_account_ids";

    @ArraySchema(schema = @Schema(implementation = String.class, example = "1,2",
            description = "gateway_account_ids that needs to be associated for the new service. Only applicable for invite type `service`"))
    private List<String> gatewayAccountIds = new ArrayList<>();

    public void setGatewayAccountIds(List<String> gatewayAccountIds) {
        this.gatewayAccountIds = gatewayAccountIds;
    }

    public List<String> getGatewayAccountIds() {
        return gatewayAccountIds;
    }
}
