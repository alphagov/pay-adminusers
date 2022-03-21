package uk.gov.pay.adminusers.queue.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SNSMessage {
    @JsonProperty("Message")
    private String message;

    public SNSMessage() {
        // for deserialization
    }

    public String getMessage() {
        return message;
    }
}
