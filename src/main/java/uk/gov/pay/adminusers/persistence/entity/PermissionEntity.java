package uk.gov.pay.adminusers.persistence.entity;

import org.eclipse.persistence.annotations.ReadOnly;
import uk.gov.pay.adminusers.model.Permission;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@ReadOnly
@Entity
@Table(name = "permissions")
public class PermissionEntity {

    @Id
    @Column(name = "id")
    private Long id;

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

    public Permission toPermission() {
        return Permission.permission(id,name,description);
    }
}
