package uk.gov.pay.adminusers.persistence.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import uk.gov.pay.adminusers.model.InviteType;

@Converter
public class InviteTypeConverter implements AttributeConverter<InviteType, String> {

    @Override
    public String convertToDatabaseColumn(InviteType inviteType) {
        return inviteType.getType();
    }

    @Override
    public InviteType convertToEntityAttribute(String string) {
        return InviteType.from(string);
    }

}
