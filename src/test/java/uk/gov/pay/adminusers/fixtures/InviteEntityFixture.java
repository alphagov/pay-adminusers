package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.time.ZonedDateTime;

import static java.time.ZonedDateTime.now;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;

public final class InviteEntityFixture {
    private Integer id = randomInt();
    private ZonedDateTime date = now();
    private ZonedDateTime expiryDate = now().plusDays(2);
    private RoleEntity role;
    private ServiceEntity service;
    private UserEntity sender;
    private String email = "someone@example.com";
    private String code = "a-code";
    private String otpKey = "otp-key";
    private String telephoneNumber;
    private String password;
    private Boolean disabled;
    private Integer loginCounter;

    private InviteEntityFixture() {
    }

    public static InviteEntityFixture anInviteEntity() {
        return new InviteEntityFixture();
    }

    public InviteEntityFixture withId(Integer id) {
        this.id = id;
        return this;
    }

    public InviteEntityFixture withDate(ZonedDateTime date) {
        this.date = date;
        return this;
    }

    public InviteEntityFixture withExpiryDate(ZonedDateTime expiryDate) {
        this.expiryDate = expiryDate;
        return this;
    }

    public InviteEntityFixture withRole(RoleEntity role) {
        this.role = role;
        return this;
    }

    public InviteEntityFixture withService(ServiceEntity service) {
        this.service = service;
        return this;
    }

    public InviteEntityFixture withSender(UserEntity sender) {
        this.sender = sender;
        return this;
    }

    public InviteEntityFixture withEmail(String email) {
        this.email = email;
        return this;
    }

    public InviteEntityFixture withCode(String code) {
        this.code = code;
        return this;
    }

    public InviteEntityFixture withOtpKey(String otpKey) {
        this.otpKey = otpKey;
        return this;
    }

    public InviteEntityFixture withTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
        return this;
    }

    public InviteEntityFixture withPassword(String password) {
        this.password = password;
        return this;
    }

    public InviteEntityFixture withDisabled(Boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    public InviteEntityFixture withLoginCounter(Integer loginCounter) {
        this.loginCounter = loginCounter;
        return this;
    }

    public InviteEntity build() {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setId(id);
        inviteEntity.setDate(date);
        inviteEntity.setExpiryDate(expiryDate);
        inviteEntity.setRole(role);
        inviteEntity.setService(service);
        inviteEntity.setSender(sender);
        inviteEntity.setEmail(email);
        inviteEntity.setCode(code);
        inviteEntity.setOtpKey(otpKey);
        inviteEntity.setTelephoneNumber(telephoneNumber);
        inviteEntity.setPassword(password);
        inviteEntity.setDisabled(disabled);
        inviteEntity.setLoginCounter(loginCounter);
        return inviteEntity;
    }
}
