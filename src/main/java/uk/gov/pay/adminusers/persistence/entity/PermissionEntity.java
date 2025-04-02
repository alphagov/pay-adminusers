package uk.gov.pay.adminusers.persistence.entity;

import org.eclipse.persistence.annotations.ReadOnly;
import uk.gov.pay.adminusers.model.Permission;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Represents a single Permission assignable to a selfservice user (Government department user)
 * <p>
 *     Marked specifically as read-only.
 *     Permissions are only intended to be added manually through migration scripts
 * </p>
 * @see RoleEntity
 */
@ReadOnly
@Entity
@Table(name = "permissions")
public class PermissionEntity {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    public PermissionEntity() {
        //for jpa
    }

    public PermissionEntity(Permission permission) {
        this.id = permission.getId();
        this.name = permission.getName();
        this.description = permission.getDescription();
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

    public Permission toPermission() {
        return Permission.permission(id,name,description);
    }
}
