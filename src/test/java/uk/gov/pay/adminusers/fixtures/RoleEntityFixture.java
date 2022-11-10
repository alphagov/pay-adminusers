package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.persistence.entity.PermissionEntity;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;

import java.util.Collections;
import java.util.Set;

import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;

public final class RoleEntityFixture {
    private Integer id = randomInt();
    private String name = "a-role";
    private String description = "A role";
    private Set<PermissionEntity> permissions = Collections.emptySet();

    private RoleEntityFixture() {
    }

    public static RoleEntityFixture aRoleEntity() {
        return new RoleEntityFixture();
    }

    public RoleEntityFixture withId(Integer id) {
        this.id = id;
        return this;
    }

    public RoleEntityFixture withName(String name) {
        this.name = name;
        return this;
    }

    public RoleEntityFixture withDescription(String description) {
        this.description = description;
        return this;
    }

    public RoleEntityFixture withPermissions(Set<PermissionEntity> permissions) {
        this.permissions = permissions;
        return this;
    }

    public RoleEntity build() {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(id);
        roleEntity.setName(name);
        roleEntity.setDescription(description);
        roleEntity.setPermissions(permissions);
        return roleEntity;
    }
}
