package uk.gov.pay.adminusers.model;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import uk.gov.pay.adminusers.utils.DateTimeUtils;

import java.io.IOException;
import java.time.ZonedDateTime;

public class ZonedDateTimeSerializer extends JsonSerializer<ZonedDateTime> {
    @Override
    public void serialize(ZonedDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        gen.writeString(DateTimeUtils.toUTCDateString(value));
    }
}
