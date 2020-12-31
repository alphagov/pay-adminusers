package uk.gov.pay.adminusers.persistence.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Converter
public class CustomBrandingConverter implements AttributeConverter<Map<String, Object>, PGobject> {
    
    private static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PGobject convertToDatabaseColumn(Map<String, Object> customBranding) {
        PGobject dbCustomBranding = new PGobject();
        dbCustomBranding.setType("json");
        try {
            dbCustomBranding.setValue(objectMapper.writeValueAsString(customBranding));
        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return dbCustomBranding;
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(PGobject dbCustomBranding) {
        try {
            if (dbCustomBranding == null || isEmpty(dbCustomBranding.toString())) {
                return null;
            } else {
                return objectMapper.readValue(dbCustomBranding.toString(), new TypeReference<>() {});
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
