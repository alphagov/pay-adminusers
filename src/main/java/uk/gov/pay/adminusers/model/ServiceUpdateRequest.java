package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class ServiceUpdateRequest {

    public static final String FIELD_OP = "op";
    public static final String FIELD_PATH = "path";
    public static final String FIELD_VALUE = "value";

    private String op;
    private String path;
    private JsonNode value;

    public String getOp() {
        return op;
    }

    public String getPath() {
        return path;
    }

    public String valueAsString() {
        if (value != null && value.isTextual()) {
            return value.asText();
        }
        return null;
    }

    public List<String> valueAsList() {
        List<String> values = new ArrayList<>();
        if (value != null && value.isArray()) {
            value.elements()
                    .forEachRemaining(node -> values.add(node.textValue()));
        }
        return values;
    }

    public Map<String, Object> valueAsObject() {
        if (value != null) {
            if ((value.isTextual() && !isEmpty(value.asText())) || value.isObject()) {
                try {
                    return new ObjectMapper().readValue(value.traverse(), new TypeReference<Map<String, Object>>() {});
                } catch (IOException e) {
                    throw new RuntimeException("Malformed JSON object in ServiceUpdateRequest.value", e);
                }
            }
        }
        return null;
    }

    public boolean valueAsBoolean() {
        return value != null && Boolean.parseBoolean(value.asText());
    }
    
    public ZonedDateTime valueAsDateTime() {
        if (value != null) {
            return ZonedDateTime.parse(value.asText());
        }
        return null;
    }


    private ServiceUpdateRequest(String op, String path, JsonNode value) {
        this.op = op;
        this.path = path;
        this.value = value;
    }

    public static ServiceUpdateRequest from(JsonNode payload) {
        return new ServiceUpdateRequest(
                payload.get(FIELD_OP).asText(),
                payload.get(FIELD_PATH).asText(),
                payload.get(FIELD_VALUE));

    }

    public static List<ServiceUpdateRequest> getUpdateRequests(JsonNode payload) {
        if (payload.isArray()) {
            List<ServiceUpdateRequest> operations = new ArrayList<>();
            payload.forEach(op -> operations.add(from(op)));
            return operations;
        } else {
            return Collections.singletonList(from(payload));
        }
    }
}
