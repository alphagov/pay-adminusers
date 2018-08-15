package uk.gov.pay.adminusers.persistence.entity.service;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class SupportedLanguageConverter implements AttributeConverter<SupportedLanguage, String> {
    @Override
    public String convertToDatabaseColumn(SupportedLanguage supportedLanguage) {
        return supportedLanguage.toString();
    }

    @Override
    public SupportedLanguage convertToEntityAttribute(String s) {
        return SupportedLanguage.fromIso639AlphaTwoCode(s);
    }
}
