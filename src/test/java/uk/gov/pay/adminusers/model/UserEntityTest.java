package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static uk.gov.pay.adminusers.model.Permission.permission;
import static uk.gov.pay.adminusers.model.Role.role;

public class UserEntityTest {

    @Test
    public void shouldConstructAUser_fromMinimalValidUserJson() throws Exception {
        String minimumUserJson = "{" +
                "\"username\": \"a-username\"," +
                "\"telephone_number\": \"+441134960000\"," +
                "\"gateway_account_ids\": [\"1\", \"2\"]," +
                "\"email\": \"email@example.com\"" +
                "}";

        JsonNode jsonNode = new ObjectMapper().readTree(minimumUserJson);
        CreateUserRequest createUserRequest = CreateUserRequest.from(jsonNode);

        UserEntity userEntity = UserEntity.from(createUserRequest);

        assertEquals(createUserRequest.getUsername(), userEntity.getUsername());
        assertEquals(createUserRequest.getPassword(), userEntity.getPassword());
        assertEquals(createUserRequest.getOtpKey(), userEntity.getOtpKey());
        assertEquals(createUserRequest.getTelephoneNumber(), userEntity.getTelephoneNumber());
        assertEquals(createUserRequest.getEmail(), userEntity.getEmail());
        assertEquals(SecondFactorMethod.SMS, userEntity.getSecondFactor());
        assertThat(userEntity.getCreatedAt(), is(notNullValue()));
        assertThat(userEntity.getUpdatedAt(), is(notNullValue()));
        // Since role and gatewayAccountId will be set up after won't be unit-testing from JSON to entity.
    }

    @Test
    public void creatingAUser_shouldSetGatewayAccountAndRole_whenServiceRoleIsSet() {
        UserEntity userEntity = new UserEntity();
        String gatewayAccountId = "1";
        ServiceEntity service = new ServiceEntity(List.of(gatewayAccountId));
        Role role = role(1, "role", "hey");
        role.setPermissions(List.of(permission(1, "perm1", "perm1 desc"), permission(2, "perm2", "perm2 desc")));
        RoleEntity roleEntity = new RoleEntity(role);
        ServiceRoleEntity serviceRole = new ServiceRoleEntity(service, roleEntity);

        userEntity.addServiceRole(serviceRole);

        assertThat(userEntity.getRoles().size(), is(1));
        assertThat(userEntity.getRoles().get(0).getId(), is(1));
        assertThat(userEntity.getGatewayAccountId(), is(gatewayAccountId));
    }
}
