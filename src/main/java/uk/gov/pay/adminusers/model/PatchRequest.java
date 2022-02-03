package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class PatchRequest {

    public static final String PATH_SESSION_VERSION = "sessionVersion";
    public static final String PATH_DISABLED = "disabled";
    public static final String PATH_TELEPHONE_NUMBER = "telephone_number";
    public static final String PATH_EMAIL = "email";
    public static final String PATH_FEATURES = "features";

    private String op;
    private String path;
    private String value;

    private PatchRequest(String op, String path, String value) {
        this.op = op;
        this.path = path;
        this.value = value;
    }

    public static PatchRequest from(JsonNode node) {
        return new PatchRequest(node.get("op").asText(), node.get("path").asText(), node.get("value").asText());
    }

    public String getOp() {
        return op;
    }
    
    public String getPath() {
        return path;
    }

    public String getValue() {
        return value;
    }
}
