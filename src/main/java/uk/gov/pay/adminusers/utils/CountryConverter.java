package uk.gov.pay.adminusers.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CountryConverter {

    public static final String COUNTRIES_FILE_PATH = "countries.json";

    private static class Country {
        private final String name;
        private String isoCode;
        private String type;
        
        Country(String name, String typeAndIsoCode) {
            this.name = name;
            String[] typeAndIsoCodeSplit = typeAndIsoCode.split(":");
            this.type = typeAndIsoCodeSplit[0];
            this.isoCode = typeAndIsoCodeSplit[1];
        }

        public String getName() {
            return name;
        }

        public boolean isCountry() {
            return type.equalsIgnoreCase("country");
        }
        
        public String getIsoCode() {
            return isoCode;
        }
    }
    private final ObjectMapper objectMapper;
    private final Map<String, String> countries;

    @Inject
    public CountryConverter(ObjectMapper objectMapper) throws IOException {
        this.objectMapper = objectMapper;
        String countries = Resources.toString(Resources.getResource(COUNTRIES_FILE_PATH), Charsets.UTF_8);
        this.countries = createMap(countries);
    }

    private Map<String, String> createMap(String countries) throws IOException {
        List<List<String>> allCountries = objectMapper.readValue(countries, new TypeReference<List>() {});
        return allCountries.stream()
                .map(a -> new Country(a.get(0), a.get(1)))
                .filter(Country::isCountry)
                .collect(Collectors.toMap(Country::getIsoCode, Country::getName)
        );
    }

    public Optional<String> getCountryNameFrom(String isoName) {
        return Optional.ofNullable(countries.get(isoName));
    }
}
