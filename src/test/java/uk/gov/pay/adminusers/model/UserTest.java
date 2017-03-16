package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;

public class UserTest {

    @Test
    public void shouldConstructAUser_fromMinimalValidUserJson() throws Exception {
        String minimumUserJson = "{" +
                "\"username\": \"a-username\"," +
                "\"telephone_number\": \"2123524\"," +
                "\"gateway_account_ids\": [\"1\", \"2\"]," +
                "\"email\": \"email@example.com\"" +
                "}";

        JsonNode jsonNode = new ObjectMapper().readTree(minimumUserJson);
        User user = User.from(jsonNode);

        assertThat(user.getUsername(), is("a-username"));
        assertThat(user.getPassword(), notNullValue());
        assertThat(user.getOtpKey(), notNullValue());
        assertThat(user.getGatewayAccountIds().size(), is(2));
        assertThat(user.getGatewayAccountIds().get(0), is("1"));
        assertThat(user.getGatewayAccountIds().get(1), is("2"));
        assertThat(user.getTelephoneNumber(), is("2123524"));
        assertThat(user.getEmail(), is("email@example.com"));
    }

    @Test
    public void shouldConstructAUser_fromCompleteValidUserJson() throws Exception {
        String minimunUserJson = "{" +
                "\"username\": \"a-username\"," +
                "\"password\": \"a-password\"," +
                "\"telephone_number\": \"2123524\"," +
                "\"gateway_account_ids\": [\"1\", \"2\"]," +
                "\"otp_key\": \"fr6ysdf\"," +
                "\"email\": \"email@example.com\"" +
                "}";

        JsonNode jsonNode = new ObjectMapper().readTree(minimunUserJson);
        User user = User.from(jsonNode);

        assertThat(user.getUsername(), is("a-username"));
        assertThat(user.getPassword(), is("a-password"));
        assertThat(user.getGatewayAccountIds().size(), is(2));
        assertThat(user.getGatewayAccountIds().get(0), is("1"));
        assertThat(user.getGatewayAccountIds().get(1), is("2"));
        assertThat(user.getTelephoneNumber(), is("2123524"));
        assertThat(user.getOtpKey(), is("fr6ysdf"));
        assertThat(user.getEmail(), is("email@example.com"));
    }

    @Test
    public void shouldFlatten_permissionsOfAUser() throws Exception {
        User user = User.from(randomInt(), "name", "password", "email@gmail.com", Arrays.asList("1"), Arrays.asList("1"), "ewrew", "453453");
        Role role1 = Role.role(1, "role1", "role1 description");
        Role role2 = Role.role(2, "role2", "role2 description");
        role1.setPermissions(ImmutableList.of(aPermission(), aPermission(), aPermission()));
        role2.setPermissions(ImmutableList.of(aPermission(), aPermission()));
        List<Role> roles = ImmutableList.of(role1, role2);
        user.setRoles(roles);

        assertThat(user.getPermissions().size(),is(5));
    }

    private Permission aPermission() {
        Integer randomInt = randomInt();
        return Permission.permission(1, "perm" + randomInt, "perm desc" + randomInt);
    }
}
