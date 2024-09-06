package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServiceNamesDeserializer extends JsonDeserializer<Map<SupportedLanguage, String>> {
    @Override
    public Map<SupportedLanguage, String> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        Map<SupportedLanguage, String> supportedLanguageToServiceName = new HashMap<>();
        jsonParser.getCodec().readValue(jsonParser, new TypeReference<Map<String, String>>() {})
                .forEach((key, value) -> 
                        supportedLanguageToServiceName.put(SupportedLanguage.fromIso639AlphaTwoCode(key), value));
        return Collections.unmodifiableMap(supportedLanguageToServiceName);
    }
}
