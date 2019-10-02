package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.ArrayList;
import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class InviteCompleteRequest {

    public static final String FIELD_GATEWAY_ACCOUNT_IDS = "gateway_account_ids";

    private List<String> gatewayAccountIds = new ArrayList<>();

    public void setGatewayAccountIds(List<String> gatewayAccountIds) {
        this.gatewayAccountIds = gatewayAccountIds;
    }

    public List<String> getGatewayAccountIds() {
        return gatewayAccountIds;
    }
}
