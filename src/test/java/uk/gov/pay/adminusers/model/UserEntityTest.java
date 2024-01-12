package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.pay.adminusers.model.Permission.permission;
import static uk.gov.pay.adminusers.model.Role.role;

class UserEntityTest {

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldConstructAUser_fromMinimalValidUserJson() throws Exception {
        String minimumUserJson = "{" +
                "\"telephone_number\": \"+441134960000\"," +
                "\"gateway_account_ids\": [\"1\", \"2\"]," +
                "\"email\": \"email@example.com\"" +
                "}";

        JsonNode jsonNode = objectMapper.readTree(minimumUserJson);
        CreateUserRequest createUserRequest = CreateUserRequest.from(jsonNode);
        String otpKey = "an-otp-key";

        UserEntity userEntity = UserEntity.from(createUserRequest, otpKey);

        assertThat(userEntity.getPassword(), is(createUserRequest.getPassword()));
        assertThat(userEntity.getOtpKey(), is(otpKey));
        assertThat(userEntity.getTelephoneNumber().get(), is(createUserRequest.getTelephoneNumber()));
        assertThat(userEntity.getEmail(), is(createUserRequest.getEmail()));
        assertThat(userEntity.getSecondFactor(), is(SecondFactorMethod.SMS));
        assertThat(userEntity.getCreatedAt(), is(notNullValue()));
        assertThat(userEntity.getUpdatedAt(), is(notNullValue()));
        // Since role and gatewayAccountId will be set up after won't be unit-testing from JSON to entity.
    }

    @Test
    void creatingAUser_shouldSetGatewayAccountAndRole_whenServiceRoleIsSet() {
        UserEntity userEntity = new UserEntity();
        String gatewayAccountId = "1";
        ServiceEntity service = new ServiceEntity(List.of(gatewayAccountId));
        Role role = role(1, "role", "hey");
        role.setPermissions(Set.of(permission(1, "perm1", "perm1 desc"), permission(2, "perm2", "perm2 desc")));
        RoleEntity roleEntity = new RoleEntity(role);
        ServiceRoleEntity serviceRole = new ServiceRoleEntity(service, roleEntity);

        userEntity.addServiceRole(serviceRole);

        assertThat(userEntity.getServicesRoles().size(), is(1));
        assertThat(userEntity.getServicesRoles().get(0).getRole().getId(), is(1));
        assertThat(userEntity.getGatewayAccountId(), is(gatewayAccountId));
    }
}
