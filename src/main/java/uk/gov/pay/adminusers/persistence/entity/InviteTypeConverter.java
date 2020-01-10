package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.model.InviteType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

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
