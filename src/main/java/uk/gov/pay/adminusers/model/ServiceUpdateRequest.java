package uk.gov.pay.adminusers.model;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

public class ServiceUpdateRequest {

    public static final String FIELD_OP = "op";
    public static final String FIELD_PATH = "path";
    public static final String FIELD_VALUE = "value";

    private String op;
    private String path;
    private List<String> value;

    public String getOp() {
        return op;
    }

    public String getPath() {
        return path;
    }

    public List<String> getValue() {
        return value;
    }

    private ServiceUpdateRequest(String op, String path, List<String> value) {
        this.op = op;
        this.path = path;
        this.value = value;
    }

    public static ServiceUpdateRequest from(JsonNode payload) {
        List<String> value = newArrayList();
        if (payload.get(FIELD_VALUE).getClass().equals(ArrayNode.class)) {
            List<JsonNode> gatewayAccountIdNodes = newArrayList(payload.get(FIELD_VALUE).elements());
            value.addAll(gatewayAccountIdNodes.stream()
                    .map(node -> node.textValue())
                    .collect(Collectors.toList()));
        } else {
            value.add(payload.get(FIELD_VALUE).asText());
        }

        return new ServiceUpdateRequest(
                payload.get(FIELD_OP).asText(),
                payload.get(FIELD_PATH).asText(),
                value);

    }
}
