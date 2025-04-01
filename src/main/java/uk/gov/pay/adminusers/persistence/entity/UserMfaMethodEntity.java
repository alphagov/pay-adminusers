package uk.gov.pay.adminusers.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import uk.gov.pay.adminusers.model.SecondFactorMethod;

import java.time.ZonedDateTime;

@Entity
@Table(name = "user_mfa_method")
@SequenceGenerator(name = "user_mfa_id_seq", sequenceName = "user_mfa_id_seq", 
        allocationSize = 1)
public class UserMfaMethodEntity extends AbstractEntity {
    @Column(name = "external_id")
    private String externalId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "description")
    private String description;

    @Column(name = "method", nullable = false)
    @Convert(converter = SecondFactorMethodConverter.class)
    private SecondFactorMethod method;
    
    @Column(name = "otp_key")
    private String otpKey;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "backup_code")
    private String backupCode;

    @Column(name = "is_primary")
    private Boolean disabled = Boolean.FALSE;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.FALSE;

    @Column(name = "created_at")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime updatedAt;

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SecondFactorMethod getMethod() {
        return method;
    }

    public void setMethod(SecondFactorMethod method) {
        this.method = method;
    }

    public String getOtpKey() {
        return otpKey;
    }

    public void setOtpKey(String otpKey) {
        this.otpKey = otpKey;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getBackupCode() {
        return backupCode;
    }

    public void setBackupCode(String backupCode) {
        this.backupCode = backupCode;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
