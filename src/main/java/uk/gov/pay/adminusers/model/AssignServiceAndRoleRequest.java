package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.dropwizard.validation.ValidationMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.WebApplicationException;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AssignServiceAndRoleRequest(
        @NotBlank(message = "Field [service_external_id] is required") String serviceExternalId,
        String roleName) {

    @ValidationMethod(message = "Field [role_name] must be one of 'admin', 'view-and-refund', 'view-only', 'view-and-initiate-moto', and 'view-refund-and-initiate-moto'")
    public boolean isValidRoleName() {
        try {
            RoleName.fromName(roleName);
            return true;
        } catch (NullPointerException | WebApplicationException e) {
            return false;
        }
    }
}
