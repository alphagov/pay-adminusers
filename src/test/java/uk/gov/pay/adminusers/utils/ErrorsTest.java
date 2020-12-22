package uk.gov.pay.adminusers.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

class ErrorsTest {

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMarshalMultipleErrorsCorrectly() throws Exception {
        List<String> errorList = List.of("Error 1", "Error 2", "Error 3");
        Errors errors = Errors.from(errorList);

        String errorsJson = objectMapper.writeValueAsString(errors);
        Map<String, List<String>> response = objectMapper.readValue(errorsJson, new TypeReference<>() {
        });

        assertThat(response.get("errors"), is(notNullValue()));
        assertThat(response.get("errors").size(), is(3));
        assertThat(response.get("errors"), hasItems("Error 1", "Error 2", "Error 3"));
    }

    @Test
    void shouldMarshalSingleErrorsCorrectly() throws Exception {
        Errors errors = Errors.from("an error");

        String errorsJson = objectMapper.writeValueAsString(errors);
        Map<String, List<String>> response = objectMapper.readValue(errorsJson, new TypeReference<>() {
        });

        assertThat(response.get("errors"), is(notNullValue()));
        assertThat(response.get("errors").size(), is(1));
        assertThat(response.get("errors"), hasItem("an error"));
    }
}
