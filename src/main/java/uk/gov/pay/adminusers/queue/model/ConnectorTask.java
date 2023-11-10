package uk.gov.pay.adminusers.queue.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "data", "task" })
public class ConnectorTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorTask.class);
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private String data;
    @JsonProperty("task")
    private String taskType;
    
    public ConnectorTask() {
        // empty
    }

    public ConnectorTask(ServiceArchivedTaskData data, String taskType) {
        try {
            this.data = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            LOGGER.info("Could not write " + data + " as a json string.", e);
            throw new RuntimeException(e);
        }
        this.taskType = taskType;
    }

    public String getTaskType() {
        return taskType;
    }

    public Object getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectorTask that = (ConnectorTask) o;
        return data.equals(that.data) && taskType.equals(that.taskType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, taskType);
    }
}
