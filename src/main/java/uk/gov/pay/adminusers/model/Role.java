package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.HashSet;
import java.util.Set;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Role {

    @JsonIgnore
    private Integer id;
    private String name; // TODO Enum this to admin, view-only, view-and-refund, super-admin
    private String description; // TODO Enum this to "Super Admin", "Administrator", "View and Refund", "View only" 
    private Set<Permission> permissions = new HashSet<>();

    public static Role role(Integer roleId, String name, String description) {
        return new Role(roleId, name, description);
    }

    private Role(Integer id, @JsonProperty("name") String name, @JsonProperty("description") String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Role role = (Role) o;

        if (!id.equals(role.id)) {
            return false;
        }

        if (!name.equals(role.name)) {
            return false;
        }

        if (!description.equals(role.description)) {
            return false;
        }

        return permissions.equals(role.permissions);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + permissions.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
