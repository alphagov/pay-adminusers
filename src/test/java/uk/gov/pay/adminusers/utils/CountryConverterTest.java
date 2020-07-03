package uk.gov.pay.adminusers.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CountryConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CountryConverter countryConverter;

    @BeforeEach
    public void setUp() throws Exception {
        this.countryConverter = new CountryConverter(
                objectMapper);
    }

    @Test
    public void shouldGetCountryNameForAValidIsoCode() {
        assertThat(countryConverter.getCountryNameFrom("AA").get(), is("aaa"));
        assertThat(countryConverter.getCountryNameFrom("BB").get(), is("bbb"));
    }

    @Test
    public void shouldNotGetCountryNameForAValidIsoCode_ifIsoCodeDoesNotRepresentACountry() {
        assertThat(countryConverter.getCountryNameFrom("CC").isPresent(), is(false));
    }


    @Test
    public void shouldNotGetCountryNameFromIsoCodeNotPresentInCountriesList() {
        assertThat(countryConverter.getCountryNameFrom("alex").isPresent(), is(false));
    }
}

