package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Permission {

    @JsonIgnore
    private Integer id;
    private String name;
    private String description;

    public static Permission permission(Integer roleId, String name, String description) {
        return new Permission(roleId, name, description);
    }

    private Permission(Integer id, @JsonProperty("name") String name, @JsonProperty("description") String description) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Permission that = (Permission) o;

        if (!id.equals(that.id)) return false;
        if (!name.equals(that.name)) return false;
        return description.equals(that.description);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + description.hashCode();
        return result;
    }
}
