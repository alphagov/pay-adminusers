package uk.gov.pay.adminusers.fixtures;

import org.apache.commons.lang3.RandomStringUtils;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.RandomStringUtils.*;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.model.Permission.permission;
import static uk.gov.pay.adminusers.model.Role.role;

public class RoleDbFixture {

    private final DatabaseTestHelper databaseTestHelper;
    private String name = "role-name-" + random(5);

    public RoleDbFixture(DatabaseTestHelper databaseTestHelper) {
        this.databaseTestHelper = databaseTestHelper;
    }

    public static RoleDbFixture aRole(DatabaseTestHelper databaseTestHelper) {
        return new RoleDbFixture(databaseTestHelper);
    }

    private Permission aPermission() {
        return permission(randomInt(), "permission-name-" + newId(), "permission-description" + newId());
    }

    public Role build() {
        Permission permission1 = aPermission();
        Permission permission2 = aPermission();
        databaseTestHelper.add(permission1).add(permission2);

        Role role = role(randomInt(), name, "role-description" + newId());
        role.setPermissions(newArrayList(permission1, permission2));
        databaseTestHelper.add(role);

        return role;
    }

    public RoleDbFixture withName(String name) {
        this.name = name;
        return this;
    }
}
