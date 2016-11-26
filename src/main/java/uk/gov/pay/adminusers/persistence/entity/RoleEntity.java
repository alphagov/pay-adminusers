package uk.gov.pay.adminusers.persistence.entity;

import org.eclipse.persistence.annotations.ReadOnly;
import uk.gov.pay.adminusers.model.Role;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ReadOnly
@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToMany(fetch = FetchType.EAGER, targetEntity = PermissionEntity.class)
    @JoinTable(name = "role_permission", joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id", referencedColumnName = "id"))
    private List<PermissionEntity> permissions = new ArrayList<>();

    public RoleEntity() {
        //for jpa
    }

    public RoleEntity(Role role) {
        this.id = role.getId();
        this.name = role.getName();
        this.description = role.getDescription();
        this.permissions = role.getPermissions().stream()
                .map(PermissionEntity::new).collect(Collectors.toList());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public List<PermissionEntity> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionEntity> permissions) {
        this.permissions = permissions;
    }

    public Role toRole() {
        Role role = Role.role(id, name, description);
        role.setPermissions(permissions.stream().map(PermissionEntity::toPermission).collect(Collectors.toList()));
        return role;
    }
}
