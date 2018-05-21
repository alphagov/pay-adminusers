package uk.gov.pay.adminusers.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CountryConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CountryConverter countryConverter;

    @Before
    public void setUp() throws Exception {
        this.countryConverter = new CountryConverter(
                objectMapper);
    }

    @Test
    public void shouldGetCountryNameFromIsoName() {
        assertThat(countryConverter.getCountryNameFrom("AA").get(), is("aaa"));
        assertThat(countryConverter.getCountryNameFrom("BB").get(), is("bbb"));
        assertThat(countryConverter.getCountryNameFrom("CC").isPresent(), is(false));
    }

    @Test
    public void shouldNotGetCountryNameFromInvalidIsoName() {
        assertThat(countryConverter.getCountryNameFrom("alex").isPresent(), is(false));
    }
}

