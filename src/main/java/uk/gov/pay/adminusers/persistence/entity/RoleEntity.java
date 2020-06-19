package uk.gov.pay.adminusers.persistence.entity;

import org.eclipse.persistence.annotations.ReadOnly;
import uk.gov.pay.adminusers.model.Role;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

/**
 * Represents a Role of a selfservice user (Government department user)
 * <p>
 *     Marked specifically as read-only.
 *     Roles are only intended to be added manually through migration scripts
 * </p>
 * @see PermissionEntity
 */
@ReadOnly
@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToMany(fetch = FetchType.EAGER, targetEntity = PermissionEntity.class)
    @JoinTable(name = "role_permission", joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id", referencedColumnName = "id"))
    private Set<PermissionEntity> permissions = new HashSet<>();

    public RoleEntity() {
        //for jpa
    }

    public RoleEntity(Role role) {
        this.id = role.getId();
        this.name = role.getName();
        this.description = role.getDescription();
        this.permissions = role.getPermissions().stream().map(PermissionEntity::new).collect(toUnmodifiableSet());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<PermissionEntity> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<PermissionEntity> permissions) {
        this.permissions = permissions;
    }

    public Role toRole() {
        Role role = Role.role(id, name, description);
        role.setPermissions(permissions.stream().map(PermissionEntity::toPermission).collect(toUnmodifiableSet()));
        return role;
    }

    public boolean isAdmin() {
        return this.id.equals(ADMIN.getId());
    }
}
