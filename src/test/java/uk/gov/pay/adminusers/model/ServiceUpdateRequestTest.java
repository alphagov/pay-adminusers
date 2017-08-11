package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ServiceUpdateRequestTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void before() throws Exception {
        objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
    }

    @Test
    public void shouldTransformToObjectCorrectly() throws Exception {
        Map<String, Object> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", ImmutableMap.of("image_url", "image url", "css_url", "css url"));
        String content = objectMapper.writeValueAsString(payload);
        JsonNode jsonNode = objectMapper.readTree(content);
        ServiceUpdateRequest request = ServiceUpdateRequest.from(jsonNode);

        Map<String, Object> objectMap = request.valueAsObject();
        assertThat(objectMap.get("image_url"), is("image url"));
        assertThat(objectMap.get("css_url"), is("css url"));
    }
}
