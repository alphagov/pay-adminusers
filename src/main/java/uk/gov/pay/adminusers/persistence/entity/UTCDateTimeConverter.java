package uk.gov.pay.adminusers.persistence.entity;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Converter
public class UTCDateTimeConverter implements AttributeConverter<ZonedDateTime, Timestamp> {

    public static final ZoneId UTC = ZoneOffset.UTC;

    @Override
    public Timestamp convertToDatabaseColumn(ZonedDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Timestamp.from(dateTime.toInstant());
    }

    @Override
    public ZonedDateTime convertToEntityAttribute(Timestamp s) {
        if (s == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(s.toInstant(), UTC);
    }
}
