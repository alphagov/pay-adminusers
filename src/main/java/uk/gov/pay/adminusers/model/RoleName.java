package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static uk.gov.pay.adminusers.service.AdminUsersExceptions.undefinedRoleException;

public enum RoleName {
    
    @JsonProperty("admin") ADMIN("admin"),
    @JsonProperty("view-and-refund") VIEW_AND_REFUND("view-and-refund"),
    @JsonProperty("view-only") VIEW_ONLY("view-only"),
    @JsonProperty("view-and-initiate-moto") VIEW_AND_INITIATE_MOTO("view-and-initiate-moto"),
    @JsonProperty("view-refund-and-initiate-moto") VIEW_REFUND_AND_INITIATE_MOTO("view-refund-and-initiate-moto"),
    @JsonProperty("super-admin") SUPER_ADMIN("super-admin");

    private static final Set<RoleName> roleNames = EnumSet.allOf(RoleName.class);
    
    private final String name;

    RoleName(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }
    
    public static RoleName fromName(String name) {
        Optional<RoleName> roleName = roleNames.stream().filter(r -> r.getName().equalsIgnoreCase(name)).findFirst();
        if (roleName.isEmpty()) {
            throw undefinedRoleException(name);
        }
        return roleName.get();
    }
}
