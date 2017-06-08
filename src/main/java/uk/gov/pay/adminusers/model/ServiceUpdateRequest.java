package uk.gov.pay.adminusers.model;


import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

import static java.util.Arrays.asList;

public class ServiceUpdateRequest {
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
        String op = payload.get("op").asText();
        String path = payload.get("path").asText();
        List<String> value = asList(payload.get("value").asText());

        return new ServiceUpdateRequest(op, path, value);
    }
}
