package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.service.payments.commons.api.exception.ValidationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InviteRequestValidatorTest {

    private static final InviteRequestValidator validator = new InviteRequestValidator();

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validateUpdateRequest_shouldNotThrowForValidRequest() {
        JsonNode request = objectMapper.valueToTree(
                List.of(
                        Map.of("path", "telephone_number",
                                "op", "replace",
                                "value", "+441134960000"),
                        Map.of("path", "password",
                                "op", "replace",
                                "value", "something-allowed")
                ));
        
        assertDoesNotThrow(() -> validator.validatePatchRequest(request));
    }

    @Test
    void validateUpdateRequest_shouldThrowWhenTelephoneNumberIsNotAString() {
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "telephone_number",
                        "op", "replace",
                        "value", 123)));
        var thrown = assertThrows(ValidationException.class, () -> validator.validatePatchRequest(request));
        assertThat(thrown.getErrors(), hasItem("Value for path [telephone_number] must be a string"));
    }

    @Test
    void validateUpdateRequest_shouldThrowWhenTelephoneNumberIsNotAValidPhoneNumber() {
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "telephone_number",
                        "op", "replace",
                        "value", "0000")));
        var thrown = assertThrows(ValidationException.class, () -> validator.validatePatchRequest(request));
        assertThat(thrown.getErrors(), hasItem("Value for path [telephone_number] must be a valid telephone number"));
    }

    @Test
    void validateUpdateRequest_shouldThrowWhenPasswordIsNotAString() {
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "password",
                        "op", "replace",
                        "value", 123)));
        var thrown = assertThrows(ValidationException.class, () -> validator.validatePatchRequest(request));
        assertThat(thrown.getErrors(), hasItem("Value for path [password] must be a string"));
    }


}
