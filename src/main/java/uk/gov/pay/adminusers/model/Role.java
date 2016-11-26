package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections.CollectionUtils.isEqualCollection;

public class Role {

    @JsonIgnore
    private Long id;
    private String name;
    private String description;
    private List<Permission> permissions = new ArrayList<>();

    public static Role role(Long roleId, String name, String description) {
        return new Role(roleId, name, description);
    }

    private Role(Long id, @JsonProperty("name") String name, @JsonProperty("description") String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Role role = (Role) o;

        if (!id.equals(role.id)) return false;
        if (!name.equals(role.name)) return false;
        if (!description.equals(role.description)) return false;
        return isEqualCollection(permissions, role.permissions);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + permissions.hashCode();
        return result;
    }
}
