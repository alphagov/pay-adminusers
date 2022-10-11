package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InviteUserRequest extends InviteRequest {

    public static final String FIELD_SENDER = "sender";
    public static final String FIELD_SERVICE_EXTERNAL_ID = "service_external_id";

    private final String sender;

    private String serviceExternalId;

    private InviteUserRequest(String sender, String email, String roleName, String serviceExternalId) {
        super(roleName, email);
        this.sender = sender;
        this.serviceExternalId = serviceExternalId;
    }

    public static InviteUserRequest from(JsonNode jsonNode) {
        return new InviteUserRequest(
                jsonNode.get(FIELD_SENDER).asText(),
                jsonNode.get(FIELD_EMAIL).asText(),
                jsonNode.get(FIELD_ROLE_NAME).asText(),
                jsonNode.get(FIELD_SERVICE_EXTERNAL_ID).asText()
        );
    }

    @Deprecated
    public static InviteUserRequest from(JsonNode jsonNode, String serviceExternalId) {
        return new InviteUserRequest(jsonNode.get(FIELD_SENDER).asText(),
                jsonNode.get(FIELD_EMAIL).asText(),
                jsonNode.get(FIELD_ROLE_NAME).asText(),
                serviceExternalId
        );
    }

    @Schema(example = "d0wksn12nklsdf1nd02nd9n2ndk", description = "User external ID", required = true)
    public String getSender() {
        return sender;
    }

    @Schema(example = "dj2jkejke32jfhh3", required = true)
    public String getServiceExternalId() {
        return serviceExternalId;
    }

}
