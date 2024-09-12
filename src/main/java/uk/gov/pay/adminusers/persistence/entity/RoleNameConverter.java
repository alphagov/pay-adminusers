package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.model.RoleName;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class RoleNameConverter implements AttributeConverter<RoleName, String> {
    @Override
    public String convertToDatabaseColumn(RoleName attribute) {
        return attribute.getName();
    }

    @Override
    public RoleName convertToEntityAttribute(String dbData) {
        return RoleName.fromName(dbData);
    }
}
