package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.service.UserServices;

import javax.ws.rs.WebApplicationException;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.resources.UserResource.CONSTRAINT_VIOLATION_MESSAGE;

public class UserResourceTest {

    private UserServices userService = mock(UserServices.class);
    private UserRequestValidator validator = mock(UserRequestValidator.class);
    private UserResource userResource = new UserResource(userService, validator);
    private JsonNode validUserNode = new ObjectMapper().valueToTree(ImmutableMap.builder()
            .put("username", "fred")
            .put("email", "user-@example.com")
            .put("gateway_account_ids", new String[]{"1", "2"})
            .put("telephone_number", "45334534634")
            .put("otp_key", "34f34")
            .put("role_name", "admin")
            .build());

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldThrowConflictExceptionIfUserNameExists() throws Exception {

        when(validator.validateCreateRequest(validUserNode)).thenReturn(Optional.empty());
        when(userService.createUser(any(User.class), eq("admin"))).thenThrow(new RuntimeException(CONSTRAINT_VIOLATION_MESSAGE));
        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 409 Conflict");
        userResource.createUser(validUserNode);

    }

    @Test
    public void shouldThrowUnknownErrorForAnyOtherReason() throws Exception {

        when(validator.validateCreateRequest(validUserNode)).thenReturn(Optional.empty());
        when(userService.createUser(any(User.class), eq("admin"))).thenThrow(new RuntimeException("Unexpected Error"));
        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("HTTP 500");
        userResource.createUser(validUserNode);

    }

    @Test
    public void shouldThrowWebApplicationExceptionIfRoleNameDoesNotExist() throws Exception {

        when(validator.validateCreateRequest(validUserNode)).thenReturn(Optional.empty());
        when(userService.createUser(any(User.class), eq("admin"))).thenThrow(new WebApplicationException("Expected Error"));
        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("Expected Error");
        userResource.createUser(validUserNode);
    }
}
