package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

class ServiceSearchRequestTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldDeserialiseCorrectly() throws JsonProcessingException {
        var searchRequest = ServiceSearchRequest.from(mapper.readTree("{\"service_name\": \"serv name\", \"service_merchant_name\": \"merchant name\"}"));
        assertThat(searchRequest.getServiceNameSearchString(), is("serv name"));
        assertThat(searchRequest.getServiceMerchantNameSearchString(), is("merchant name"));
    }

    @Test
    void shouldSetDefaultEmptyStrings_onMalformedJSON() throws JsonProcessingException {
        var searchRequest = ServiceSearchRequest.from(mapper.readTree("{\"random_key\": \"random val\", \"random_key2\": \"random val\"}"));
        assertThat(searchRequest.getServiceNameSearchString(), is(""));
        assertThat(searchRequest.getServiceMerchantNameSearchString(), is(""));
    }

    @Test
    void shouldReturnMapOfValues() throws JsonProcessingException {
        var searchRequest = ServiceSearchRequest.from(mapper.readTree("{\"service_name\": \"serv name\", \"service_merchant_name\": \"merchant name\"}"));
        var mapOfRequest = searchRequest.toMap();
        mapOfRequest.keySet().forEach(key -> assertThat(mapOfRequest.get(key), anyOf(equalTo("serv name"), equalTo("merchant name"))));
    }
}
