package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public abstract class InviteRequest {

    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_ROLE_NAME = "role_name";

    protected final String roleName;
    protected final String email;

    public InviteRequest(String roleName, String email) {
        this.roleName = roleName;
        this.email = email;
    }

    @Schema(example = "example@example.gov.uk", required = true)
    public String getEmail() {
        return email;
    }

    @Schema(example = "view-only", required = true)
    public String getRoleName() {
        return roleName;
    }
}
