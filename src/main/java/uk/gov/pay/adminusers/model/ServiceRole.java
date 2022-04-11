package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ServiceRole {

    private Service service;
    private Role role;

    public static ServiceRole from(Service service, Role role) {
        return new ServiceRole(service, role);
    }

    private ServiceRole(@JsonProperty("service") Service service, @JsonProperty("role") Role role) {
        this.service = service;
        this.role = role;
    }

    public Service getService() {
        return service;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "ServiceRole{" +
                "Service=" + service +
                ", role=" + role +
                '}';
    }
}
