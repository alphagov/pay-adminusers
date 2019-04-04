package uk.gov.pay.adminusers.model;

import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.pay.commons.model.SupportedLanguage;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServiceName {

    private final String englishServiceName;
    private final Map<SupportedLanguage, String> translatedServiceNames;

    public ServiceName(String englishServiceName) {
        this(englishServiceName, Collections.emptyMap());
    }

    public ServiceName(String englishServiceName, Map<SupportedLanguage, String> translatedServiceNames) {
        if (translatedServiceNames.containsKey(SupportedLanguage.ENGLISH)) {
            throw new IllegalArgumentException("Specify the English service name as the first argument only and not in the map please");
        }

        this.englishServiceName = Objects.requireNonNull(englishServiceName);

        this.translatedServiceNames = translatedServiceNames
                .entrySet()
                .stream()
                .filter(entry -> !entry.getValue().isBlank())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public String getEnglish() {
        return englishServiceName;
    }

    public Map<SupportedLanguage, String> getEnglishAndTranslations() {
        return Stream.concat(
                Map.of(SupportedLanguage.ENGLISH, englishServiceName).entrySet().stream(), translatedServiceNames.entrySet().stream())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static ServiceName from(Collection<ServiceNameEntity> serviceNameEntities) {
        String englishServiceName = serviceNameEntities
                .stream()
                .filter(serviceNameEntity -> serviceNameEntity.getLanguage() == SupportedLanguage.ENGLISH)
                .findFirst()
                .map(ServiceNameEntity::getName)
                .orElseThrow(() -> new IllegalArgumentException("No English-language service name provided"));

        Map<SupportedLanguage, String> translatedServiceNames = serviceNameEntities
                .stream()
                .filter(serviceNameEntity -> serviceNameEntity.getLanguage() != SupportedLanguage.ENGLISH)
                .collect(Collectors.toUnmodifiableMap(ServiceNameEntity::getLanguage, ServiceNameEntity::getName));

        return new ServiceName(englishServiceName, translatedServiceNames);
    }

}
