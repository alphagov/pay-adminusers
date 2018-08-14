package uk.gov.pay.adminusers.persistence.entity.service;

public enum SupportedLanguage {

    ENGLISH("en"),
    WELSH("cy");

    private final String iso639AlphaTwoCode;

    SupportedLanguage(String iso639AlphaTwoCode) {
        this.iso639AlphaTwoCode = iso639AlphaTwoCode;
    }

    @Override
    public String toString() {
        return iso639AlphaTwoCode;
    }

    public static SupportedLanguage fromIso639AlphaTwoCode(String iso639AlphaTwoCode) {
        for (SupportedLanguage supportedLanguage : SupportedLanguage.values()) {
            if (supportedLanguage.iso639AlphaTwoCode.equals(iso639AlphaTwoCode)) {
                return supportedLanguage;
            }
        }
        throw new IllegalArgumentException(iso639AlphaTwoCode + " is not a supported ISO 639-1 code");
    }

}
