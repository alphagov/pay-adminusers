package uk.gov.pay.adminusers.model;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.Role.role;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

public class UserTest {

    @Test
    public void shouldFlatten_permissionsOfAUser() throws Exception {
        Service service = Service.from(1, "3487347gb67", Service.DEFAULT_NAME_VALUE);
        User user = User.from(randomInt(), randomUuid(), "name", "password", "email@example.com", asList("1"), asList(service), "ewrew", "453453",
                asList(ServiceRole.from(service, role(ADMIN.getId(), "Admin", "Administrator"))));
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
