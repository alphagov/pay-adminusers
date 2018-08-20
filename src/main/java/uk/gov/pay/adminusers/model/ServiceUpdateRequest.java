package uk.gov.pay.adminusers.model;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class ServiceUpdateRequest {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(ServiceUpdateRequest.class);

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
        if (value != null && value.isArray()) {
            return newArrayList(value.elements())
                    .stream()
                    .map(node -> node.textValue())
                    .collect(toList());
        }
        return null;
    }

    public Map<String, Object> valueAsObject() {
        if (value != null) {
            if ((value.isTextual() && !isEmpty(value.asText())) || value.isObject()) {
                try {
                    return new ObjectMapper().readValue(value.traverse(), new TypeReference<Map<String, Object>>() {});
                } catch (IOException e) {
                    throw new RuntimeException(format("Malformed JSON object in ServiceUpdateRequest.value"), e);
                }
            }
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
        try {
            JsonNode jsonArray = new ObjectMapper().readTree(payload.toString());
            List<ServiceUpdateRequest> operations = new ArrayList<>();
            jsonArray.forEach(op -> operations.add(from(op)));
            return operations;
        } catch (IOException e) {
            LOGGER.info("There was an exception processing update request [{}]", e.getMessage());
            return Collections.emptyList();
        }
    }
}
