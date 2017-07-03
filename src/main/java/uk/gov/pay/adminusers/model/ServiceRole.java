package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ServiceRole {

    private Service Service;
    private Role role;

    public static ServiceRole from(Service service, Role role) {
        return new ServiceRole(service, role);
    }

    private ServiceRole(@JsonProperty("service") Service service, @JsonProperty("role") Role role) {
        Service = service;
        this.role = role;
    }

    public Service getService() {
        return Service;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "ServiceRole{" +
                "Service=" + Service +
                ", role=" + role +
                '}';
    }
}
