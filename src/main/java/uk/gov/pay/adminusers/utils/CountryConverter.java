package uk.gov.pay.adminusers.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.google.inject.Inject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toUnmodifiableMap;

public class CountryConverter {

    private static final String COUNTRIES_FILE_PATH = "countries.json";
    private final ObjectMapper objectMapper;
    private final Map<String, String> countries;

    @Inject
    public CountryConverter(ObjectMapper objectMapper) throws IOException {
        this.objectMapper = objectMapper;
        String textCountries = Resources.toString(Resources.getResource(COUNTRIES_FILE_PATH), UTF_8);
        this.countries = createMap(textCountries);
    }

    private Map<String, String> createMap(String countries) throws IOException {
        List<List<String>> allCountries = objectMapper.readValue(countries, new TypeReference<>() {});
        return allCountries.stream()
                .filter(country -> country.get(1).startsWith("country:"))
                .collect(toUnmodifiableMap(
                        country -> getIsoCode(country.get(1)),
                        country -> country.get(0)
                ));
    }

    private static String getIsoCode(String typeAndIsoCode) {
        return typeAndIsoCode.substring(typeAndIsoCode.indexOf(':') + 1);
    }
    
    public Optional<String> getCountryNameFrom(String isoName) {
        return Optional.ofNullable(countries.get(isoName));
    }
}
