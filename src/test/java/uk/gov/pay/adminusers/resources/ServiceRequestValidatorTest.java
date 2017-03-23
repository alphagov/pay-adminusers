package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import uk.gov.pay.adminusers.utils.Errors;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.resources.ServiceResource.FIELD_NEW_SERVICE_NAME;

public class ServiceRequestValidatorTest {

    ServiceRequestValidator serviceValidator = new ServiceRequestValidator(new RequestValidations());

    @Test
    public void shouldPassValidation() throws Exception{
        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode newNameMock = mock(JsonNode.class);
        when(jsonNode.get(FIELD_NEW_SERVICE_NAME)).thenReturn(newNameMock);
        when(newNameMock.asText()).thenReturn(RandomStringUtils.randomAlphanumeric(20));

        Optional<Errors> optionalErrors = serviceValidator.validateUpdateRequest(jsonNode);

        assertThat(optionalErrors.isPresent(), is(false));
    }


    @Test
    public void shouldReturnErrors_ifNewServiceNameExceedingAllowedLength(){
        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode newNameMock = mock(JsonNode.class);
        when(jsonNode.get(FIELD_NEW_SERVICE_NAME)).thenReturn(newNameMock);
        when(newNameMock.asText()).thenReturn(RandomStringUtils.randomAlphanumeric(51));

        Optional<Errors> optionalErrors = serviceValidator.validateUpdateRequest(jsonNode);

        assertThat(optionalErrors.get().getErrors().size(), is(1));
        assertThat(optionalErrors.get().getErrors(), hasItem("Field [new_service_name] must have a maximum length of 50 characters"));
    }

    @Test
    public void shouldReturnErrors_ifNewServiceNameIsInvalidJsonNode(){
        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode newNameMock = mock(JsonNode.class);
        when(jsonNode.get("service_name")).thenReturn(newNameMock);
        when(newNameMock.asText()).thenReturn(RandomStringUtils.randomAlphanumeric(256));

        Optional<Errors> optionalErrors = serviceValidator.validateUpdateRequest(jsonNode);

        assertThat(optionalErrors.get().getErrors().size(), is(1));
        assertThat(optionalErrors.get().getErrors(), hasItem("Field [new_service_name] is required"));
    }

    @Test
    public void shouldReturnErrors_ifNewJsonPayloadIsNull(){
        Optional<Errors> optionalErrors = serviceValidator.validateUpdateRequest(null);

        assertThat(optionalErrors.get().getErrors().size(), is(1));
        assertThat(optionalErrors.get().getErrors(), hasItem("invalid JSON"));
    }

    @Test
    public void shouldReturnErrors_ifNewServiceNameIsEmpty(){
        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode newNameMock = mock(JsonNode.class);
        when(jsonNode.get(FIELD_NEW_SERVICE_NAME)).thenReturn(newNameMock);
        when(newNameMock.asText()).thenReturn("   ");

        Optional<Errors> optionalErrors = serviceValidator.validateUpdateRequest(jsonNode);

        assertThat(optionalErrors.get().getErrors().size(), is(1));
        assertThat(optionalErrors.get().getErrors(), hasItem("Field [new_service_name] is required"));
    }
}
