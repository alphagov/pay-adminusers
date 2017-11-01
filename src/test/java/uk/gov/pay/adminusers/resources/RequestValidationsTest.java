package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class RequestValidationsTest {

    private RequestValidations requestValidations = new RequestValidations();

    private static final String FIELD_1 = "field1";
    private static final String FIELD_2 = "field2";

    @Test
    public void checkIfExists_shouldSuccess_whenFieldsAreProvidedWithValues() throws Exception {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put(FIELD_1, "value1");
        payload.put(FIELD_2, "value2");

        Optional<List<String>> errors = requestValidations.checkIfExistsOrEmpty(payload, FIELD_1, FIELD_2);

        assertThat(errors.isPresent(), is(false));
    }

    @Test
    public void checkIfExists_shouldFail_whenFieldsAreProvidedWithEmptyValues() throws Exception {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put(FIELD_1, "");
        payload.put(FIELD_2, "");

        Optional<List<String>> errors = requestValidations.checkIfExistsOrEmpty(payload, FIELD_1, FIELD_2);

        assertThat(errors.isPresent(), is(true));
    }

    @Test
    public void checkIfExists_shouldFail_whenFieldsAreProvidedWithNullValues() throws Exception {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.set(FIELD_1, null);
        payload.set(FIELD_2, null);
        System.out.println(payload);

        Optional<List<String>> errors = requestValidations.checkIfExistsOrEmpty(payload, FIELD_1, FIELD_2);

        assertThat(errors.isPresent(), is(true));
    }

}
