package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.service.payments.commons.api.json.ApiResponseDateTimeSerializer;

import java.time.ZonedDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserMfa {

    @Schema(example = "mfa_93ba1ec4ed6a4238a59f16ad97b4fa12")
    private String externalId;
    @Schema(example = "google authenticator")
    private String description;
    @Schema(example = "447700900000")
    private String phoneNumber;
    @Schema(example = "MFA method. sms, backup_code, app")
    private SecondFactorMethod method;
    @Schema(example = "false")
    private Boolean isPrimary = Boolean.FALSE;
    @Schema(example = "false")
    private Boolean isActive = Boolean.FALSE;
    @Schema(example = "2022-04-06T23:03:41.665Z")
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    private ZonedDateTime updatedAt;
    @Schema(example = "2022-04-06T23:03:41.665Z")
    @JsonSerialize(using = ApiResponseDateTimeSerializer.class)
    private ZonedDateTime createdAt;

    public String getExternalId() {
        return externalId;
    }

    public String getDescription() {
        return description;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }



    public Boolean getPrimary() {
        return isPrimary;
    }

    public Boolean getActive() {
        return isActive;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setPrimary(Boolean primary) {
        isPrimary = primary;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public SecondFactorMethod getMethod() {
        return method;
    }

    public void setMethod(SecondFactorMethod method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "UserMfa{" +
                "externalId='" + externalId + '\'' +
                ", description='" + description + '\'' +
                ", method=" + method +
                ", isActive=" + isActive +
                ", isPrimary=" + isPrimary +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
