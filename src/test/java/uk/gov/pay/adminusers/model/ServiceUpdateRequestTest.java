package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ServiceUpdateRequestTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void before() {
        objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
    }

    @Test
    public void shouldTransformToObjectCorrectly() throws IOException {
        Map<String, Object> payload = ImmutableMap.of("path", "custom_branding", "op", "replace", "value", ImmutableMap.of("image_url", "image url", "css_url", "css url"));
        String content = objectMapper.writeValueAsString(payload);
        JsonNode jsonNode = objectMapper.readTree(content);
        ServiceUpdateRequest request = ServiceUpdateRequest.from(jsonNode);

        Map<String, Object> objectMap = request.valueAsObject();
        assertThat(objectMap.get("image_url"), is("image url"));
        assertThat(objectMap.get("css_url"), is("css url"));
    }

    @Test
    public void shouldReturnAList_whenJsonIsArray() throws IOException {
        String jsonPayload = fixture("fixtures/resource/service/patch/array-replace-name-replace-service-name-cy.json");

        final List<ServiceUpdateRequest> requests = ServiceUpdateRequest.getUpdateRequests(new ObjectMapper().readTree(jsonPayload));

        assertThat(requests.size(), is(2));
        requests.sort(Comparator.comparing(ServiceUpdateRequest::getPath));
        assertThat(requests.get(0).getPath(), is("name"));
        assertThat(requests.get(0).getOp(), is("replace"));

        assertThat(requests.get(1).getPath(), is("service_name/cy"));
        assertThat(requests.get(1).getOp(), is("replace"));
    }

    @Test
    public void shouldReturnAList_whenJsonIsSingleObject() throws IOException {
        //language=JSON
        String jsonPayload =
                "{\n" +
                        "  \"op\": \"replace\",\n" +
                        "  \"path\": \"name\",\n" +
                        "  \"value\": \"new-en-name\"\n" +
                        "}\n";

        final List<ServiceUpdateRequest> requests = ServiceUpdateRequest.getUpdateRequests(new ObjectMapper().readTree(jsonPayload));

        assertThat(requests.size(), is(1));
        assertThat(requests.get(0).getPath(), is("name"));
    }
}
