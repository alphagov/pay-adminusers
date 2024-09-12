package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toUnmodifiableMap;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.undefinedRoleException;

public enum RoleName {
    
    @JsonProperty("admin") ADMIN("admin"),
    @JsonProperty("view-and-refund") VIEW_AND_REFUND("view-and-refund"),
    @JsonProperty("view-only") VIEW_ONLY("view-only"),
    @JsonProperty("view-and-initiate-moto") VIEW_AND_INITIATE_MOTO("view-and-initiate-moto"),
    @JsonProperty("view-refund-and-initiate-moto") VIEW_REFUND_AND_INITIATE_MOTO("view-refund-and-initiate-moto"),
    @JsonProperty("super-admin") SUPER_ADMIN("super-admin");

    private final String name;

    private static final Map<String, RoleName> roleNames = Arrays.stream(RoleName.values())
            .collect(toUnmodifiableMap(RoleName::getName, Function.identity()));
    
    RoleName(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }
    
    public static RoleName fromName(String name) {
        return Optional.ofNullable(roleNames.get(name)).orElseThrow(() -> undefinedRoleException(name));
    }
}
