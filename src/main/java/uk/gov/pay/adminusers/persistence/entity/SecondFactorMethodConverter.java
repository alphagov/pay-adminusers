package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.model.SecondFactorMethod;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SecondFactorMethodConverter implements AttributeConverter<SecondFactorMethod, String> {

    @Override
    public String convertToDatabaseColumn(SecondFactorMethod secondFactor) {
        return secondFactor.toString();
    }

    @Override
    public SecondFactorMethod convertToEntityAttribute(String string) {
        for (SecondFactorMethod secondFactorMethod : SecondFactorMethod.values()) {
            if (secondFactorMethod.toString().equals(string)) {
                return secondFactorMethod;
            }
        }
        return SecondFactorMethod.SMS;
    }

}
