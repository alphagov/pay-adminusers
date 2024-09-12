package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashSet;
import java.util.Set;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Role {

    @JsonIgnore
    private Integer id;
    
    @Schema(example = "admin")
    @JsonProperty("name")
    private RoleName roleName;
    
    @Schema(example = "Administrator")
    private String description; // TODO Enum this to "Super Admin", "Administrator", "View and Refund", "View only" 
    
    private Set<Permission> permissions = new HashSet<>();

    public Role(Integer id, @JsonProperty("name") RoleName roleName, @JsonProperty("description") String description) {
        this.id = id;
        this.roleName = roleName;
        this.description = description;
    }

    public Integer getId() {
        return id;
    }

    public RoleName getRoleName() {
        return roleName;
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

        if (!roleName.equals(role.roleName)) {
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
        result = 31 * result + roleName.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + permissions.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + roleName + '\'' +
                '}';
    }
}
