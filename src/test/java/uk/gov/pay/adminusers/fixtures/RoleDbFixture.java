package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.model.Permission.permission;
import static uk.gov.pay.adminusers.model.Role.role;

public class RoleDbFixture {

    private final DatabaseTestHelper databaseHelper;
    private String name = "role-name-" + random(5);

    public RoleDbFixture(DatabaseTestHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public static RoleDbFixture roleDbFixture(DatabaseTestHelper databaseHelper) {
        return new RoleDbFixture(databaseHelper);
    }

    private Permission aPermission() {
        return permission(randomInt(), "permission-name-" + newId(), "permission-description" + newId());
    }

    public Role insertRole() {
        Permission permission1 = aPermission();
        Permission permission2 = aPermission();
        databaseHelper.add(permission1).add(permission2);

        Role role = role(randomInt(), name, "role-description" + newId());
        role.setPermissions(newArrayList(permission1, permission2));
        databaseHelper.add(role);

        return role;
    }

    public RoleDbFixture withName(String name) {
        this.name = name;
        return this;
    }
}
