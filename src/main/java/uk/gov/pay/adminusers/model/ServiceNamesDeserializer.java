package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.io.IOException;
import java.util.Map;

import static java.util.stream.Collectors.toUnmodifiableMap;

public class ServiceNamesDeserializer extends JsonDeserializer<Map<SupportedLanguage, String>> {
    @Override
    public Map<SupportedLanguage, String> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return jsonParser.getCodec().readValue(jsonParser, new TypeReference<Map<String, String>>() {})
                .entrySet().stream()
                .collect(toUnmodifiableMap(entry -> SupportedLanguage.fromIso639AlphaTwoCode(entry.getKey()), Map.Entry::getValue));
    }
}
