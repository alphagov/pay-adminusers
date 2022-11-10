package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.app.util.RandomIdGenerator;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.service.AdminUsersExceptions;
import uk.gov.pay.adminusers.utils.telephonenumber.TelephoneNumberUtility;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.DAYS;
import static uk.gov.pay.adminusers.model.InviteType.USER;
import static uk.gov.pay.adminusers.persistence.entity.UTCDateTimeConverter.UTC;

@Entity
@Table(name = "invites")
public class InviteEntity extends AbstractEntity {

    private static final long EXPIRY_DAYS = 2L;

    @Column(name = "date")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime date;

    @Column(name = "expiry_date")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime expiryDate;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private ServiceEntity service;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private UserEntity sender;

    @Column(name = "email")
    private String email;

    @Column(name = "code")
    private String code;

    @Column(name = "otp_key")
    private String otpKey;

    @Column(name = "telephone_number")
    private String telephoneNumber;

    @Column(name = "password")
    private String password;

    @Column(name = "disabled")
    private Boolean disabled = Boolean.FALSE;

    @Column(name = "login_counter")
    private Integer loginCounter = 0;

    @Column(name = "type")
    @Convert(converter = InviteTypeConverter.class)
    private InviteType type = USER;

    /**
     * For JPA
     */
    public InviteEntity() {
        super();
    }

    public InviteEntity(String email, String code, String otpKey, RoleEntity role) {
        super();
        this.date = ZonedDateTime.now(ZoneId.of("UTC"));
        initializeExpiry();
        this.code = code;
        this.otpKey = otpKey;
        this.email = email.toLowerCase(Locale.ENGLISH);
        this.role = role;
    }

    /**
     * Being:
     * <p>
     * 'X' the moment the invite is created and
     * '|' = 00:00 of the following day
     * '^' the moment it expires
     * 'N' = Now
     * <p>
     * <-------Day 0---------><-------Day 1---------><-------Day 2--------->
     * |----------------------|----------------------|----------------------|
     * X                               N             ^
     * <p>
     * Invite created Day 1 -> 00:00:00:000 will expired at Day 3 -> 00:00:00:000
     */
    private void initializeExpiry() {
        this.expiryDate = this.date.truncatedTo(DAYS).plus(EXPIRY_DAYS, DAYS);
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public void setExpiryDate(ZonedDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public ZonedDateTime getExpiryDate() {
        return expiryDate;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getOtpKey() {
        return otpKey;
    }

    public void setOtpKey(String otpKey) {
        this.otpKey = otpKey;
    }

    public Optional<RoleEntity> getRole() {
        return Optional.ofNullable(role);
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    public Optional<ServiceEntity> getService() {
        return Optional.ofNullable(service);
    }

    public void setService(ServiceEntity service) {
        this.service = service;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserEntity getSender() {
        return sender;
    }

    public void setSender(UserEntity sender) {
        this.sender = sender;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public void setTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Integer getLoginCounter() {
        return loginCounter;
    }

    public void setLoginCounter(Integer loginCount) {
        this.loginCounter = loginCount;
    }

    public InviteType getType() {
        return type;
    }

    public void setType(InviteType type) {
        this.type = type;
    }

    public boolean isServiceType() {
        return InviteType.SERVICE.equals(type);
    }

    public boolean isUserType() {
        return InviteType.USER.equals(type);
    }

    public Invite toInvite() {
        String roleName = getRole().map(RoleEntity::getName).orElse(null);
        return new Invite(code, email, telephoneNumber, disabled, loginCounter, type.getType(), roleName, isExpired(), hasPassword());
    }

    public boolean isExpired() {
        return ZonedDateTime.now(UTC).isAfter(expiryDate);
    }

    private boolean hasPassword() {
        return password != null;
    }

    public UserEntity mapToUserEntity() {
        UserEntity userEntity = new UserEntity();
        userEntity.setExternalId(RandomIdGenerator.randomUuid());
        userEntity.setUsername(email);
        userEntity.setPassword(password);
        userEntity.setEmail(email);
        userEntity.setOtpKey(otpKey);
        if (telephoneNumber != null) {
            userEntity.setTelephoneNumber(TelephoneNumberUtility.formatToE164(telephoneNumber));
        }
        userEntity.setSecondFactor(SecondFactorMethod.SMS);
        userEntity.setLoginCounter(0);
        userEntity.setDisabled(Boolean.FALSE);
        userEntity.setSessionVersion(0);
        this.getService().ifPresent(serviceEntity -> {
            RoleEntity roleEntity = this.getRole().orElseThrow(() -> AdminUsersExceptions.inviteDoesNotHaveRole(code));
            userEntity.addServiceRole(new ServiceRoleEntity(serviceEntity, roleEntity));
        });
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        userEntity.setCreatedAt(now);
        userEntity.setUpdatedAt(now);
        return userEntity;
    }
}
