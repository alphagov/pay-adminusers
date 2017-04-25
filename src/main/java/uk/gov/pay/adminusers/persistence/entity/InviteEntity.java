package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.model.Invite;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;
import static uk.gov.pay.adminusers.persistence.entity.UTCDateTimeConverter.UTC;

@Entity
@Table(name = "invites")
public class InviteEntity extends AbstractEntity {

    private static final long EXPIRY_DAYS = 2L;

    @Column(name = "date")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime date;

    @ManyToOne
    @JoinColumn(name = "role_id")
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

    public InviteEntity() {
        //for jpa
    }

    public InviteEntity(String email, String code, UserEntity sender, ServiceEntity service, RoleEntity role) {
        this.service = service;
        this.date = now(ZoneId.of("UTC"));
        this.code = code;
        this.otpKey = newId();
        this.sender = sender;
        this.email = email.toLowerCase();
        this.role = role;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
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

    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    public ServiceEntity getService() {
        return service;
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

    public Invite toInvite(String inviteUrl) {
        return new Invite(email, inviteUrl);
    }

    public Invite toInvite() {
        return new Invite(email);
    }

    /**
     * isExpired()
     * <p>
     * Calculates expire based on midnight (hence the truncation to days that sets the day to Midnight)
     * It takes the date when the Invite was created and truncate it to days (set it to Midnight), adds the EXPIRY_DAYS
     * calculates if this date result is after Now. IE:
     * <p>
     * - Invite created 01-05-2017 10:15:20
     * - isExpired() checked on 02-05-2017 17:50:10
     * <p>
     * - Truncating Invite created -> 01-05-2017 00:00:00
     * - Being EXPIRY_DAYS = 2, SUM these to the previous days -> 03-05-2017 00:00:00
     * <p>
     * - Note: EXPIRY_DAYS = 2 is equivalent to next day Midnight (the following day after next at 00:00:00) so the
     * Invite will be valid until next day until 23:59:59:999
     * <p>
     * - Invite is not expired because 02-05-2017 17:50:10 < 03-05-2017 00:00:00
     *
     * @return true if expired
     */
    public boolean isExpired() {
        return now(UTC).isAfter(date.truncatedTo(DAYS).plus(EXPIRY_DAYS, DAYS));
    }
}
