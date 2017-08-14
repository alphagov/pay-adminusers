package uk.gov.pay.adminusers.persistence.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Converter
public class CustomBrandingConverter implements AttributeConverter<Map<String, Object>, PGobject> {
    @Override
    public PGobject convertToDatabaseColumn(Map<String, Object> customBranding) {
        PGobject dbCustomBranding = new PGobject();
        dbCustomBranding.setType("json");
        try {
            dbCustomBranding.setValue(new ObjectMapper().writeValueAsString(customBranding));
        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return dbCustomBranding;
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(PGobject dbCustomBranding) {
        try {
            if (dbCustomBranding != null && !isEmpty(dbCustomBranding.toString())) {
                return new ObjectMapper().readValue(dbCustomBranding.toString(), new TypeReference<Map<String, Object>>() {});
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
