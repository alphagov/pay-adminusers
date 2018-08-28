package uk.gov.pay.adminusers.persistence.entity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.pay.adminusers.model.ServiceUpdateRequest;
import uk.gov.pay.commons.model.SupportedLanguage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ServiceNameEntityTest {

    @Test
    public void fromServiceUpdateRequest_shouldCreateANewEntity() throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("op", "replace");
        payload.put("path", "name");
        payload.put("value", ImmutableMap.of("en", "new-en-name"));

        final ObjectMapper mapper = new ObjectMapper();
        final String stringPayload = mapper.writeValueAsString(payload);
        ServiceUpdateRequest serviceUpdateRequest = ServiceUpdateRequest.from(mapper.readTree(stringPayload));

        ServiceNameEntity serviceNameEntity = ServiceNameEntity.from(serviceUpdateRequest);
        assertThat(serviceNameEntity.getName(), is("new-en-name"));
        assertThat(serviceNameEntity.getLanguage(), is(SupportedLanguage.ENGLISH));
    }
}
