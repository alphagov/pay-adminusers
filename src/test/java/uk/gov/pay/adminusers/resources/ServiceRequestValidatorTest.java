package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.Optional;

import static org.junit.Assert.assertFalse;

public class ServiceRequestValidatorTest {

    private ObjectMapper mapper = new ObjectMapper();
    private ServiceRequestValidator serviceRequestValidator = new ServiceRequestValidator(new RequestValidations());

    @Test
    public void shouldSuccess_onEmptyJson() throws Exception {
        Optional<Errors> errors = serviceRequestValidator.validateCreateRequest(mapper.readTree("{}"));
        assertFalse(errors.isPresent());

    }

    @Test
    public void shouldSuccess_onNullPayload() throws Exception {
        Optional<Errors> errors = serviceRequestValidator.validateCreateRequest(null);
        assertFalse(errors.isPresent());
    }

    @Test
    public void shouldSuccess_ifNameIsEmpty() throws Exception {
        ImmutableMap<String, String> payload = ImmutableMap.of("name", "");
        Optional<Errors> errors = serviceRequestValidator.validateCreateRequest(mapper.valueToTree(payload));
        assertFalse(errors.isPresent());
    }

    @Test
    public void shouldSuccess_ifGatewayAccountIdsIsEmptyArray() throws Exception {
        ImmutableMap<Object, Object> payload = ImmutableMap.builder()
                .put("gateway_account_ids", new String[]{})
                .build();

        Optional<Errors> errors = serviceRequestValidator.validateCreateRequest(mapper.valueToTree(payload));
        assertFalse(errors.isPresent());
    }

    @Test
    public void shouldSuccess_forValidGatewayAccountIds() throws Exception {

        ImmutableMap<Object, Object> payload = ImmutableMap.builder()
                .put("gateway_account_ids", new String[]{"1","2"})
                .build();

        Optional<Errors> errors = serviceRequestValidator.validateCreateRequest(mapper.valueToTree(payload));
        assertFalse(errors.isPresent());

    }

}
