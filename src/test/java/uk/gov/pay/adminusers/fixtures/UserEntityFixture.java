package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.time.ZonedDateTime;

import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;

public final class UserEntityFixture {
    private Integer id = randomInt();
    private String externalId = "an-external-id";
    private String username = "foo@example.com";
    private String password;
    private String email = "foo@example.com";
    private String otpKey = "an-otp-key";
    private String telephoneNumber;
    private Boolean disabled;
    private Integer loginCounter;
    private String features;
    private ZonedDateTime createdAt = ZonedDateTime.parse("2022-01-01T00:00:00Z");
    private ZonedDateTime updatedAt = ZonedDateTime.parse("2022-01-01T00:00:00Z");;
    private Integer sessionVersion = 1;
    private SecondFactorMethod secondFactor = SecondFactorMethod.SMS;
    private String provisionalOtpKey;
    private ZonedDateTime provisionalOtpKeyCreatedAt;
    private ZonedDateTime lastLoggedInAt;

    private UserEntityFixture() {
    }

    public static UserEntityFixture aUserEntity() {
        return new UserEntityFixture();
    }

    public UserEntityFixture withId(Integer id) {
        this.id = id;
        return this;
    }

    public UserEntityFixture withExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public UserEntityFixture withUsername(String username) {
        this.username = username;
        return this;
    }

    public UserEntityFixture withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserEntityFixture withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserEntityFixture withOtpKey(String otpKey) {
        this.otpKey = otpKey;
        return this;
    }

    public UserEntityFixture withTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
        return this;
    }

    public UserEntityFixture withDisabled(Boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    public UserEntityFixture withLoginCounter(Integer loginCounter) {
        this.loginCounter = loginCounter;
        return this;
    }

    public UserEntityFixture withFeatures(String features) {
        this.features = features;
        return this;
    }

    public UserEntityFixture withCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public UserEntityFixture withUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public UserEntityFixture withSessionVersion(Integer sessionVersion) {
        this.sessionVersion = sessionVersion;
        return this;
    }

    public UserEntityFixture withSecondFactor(SecondFactorMethod secondFactor) {
        this.secondFactor = secondFactor;
        return this;
    }

    public UserEntityFixture withProvisionalOtpKey(String provisionalOtpKey) {
        this.provisionalOtpKey = provisionalOtpKey;
        return this;
    }

    public UserEntityFixture withProvisionalOtpKeyCreatedAt(ZonedDateTime provisionalOtpKeyCreatedAt) {
        this.provisionalOtpKeyCreatedAt = provisionalOtpKeyCreatedAt;
        return this;
    }

    public UserEntityFixture withLastLoggedInAt(ZonedDateTime lastLoggedInAt) {
        this.lastLoggedInAt = lastLoggedInAt;
        return this;
    }

    public UserEntity build() {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(id);
        userEntity.setExternalId(externalId);
        userEntity.setUsername(username);
        userEntity.setPassword(password);
        userEntity.setEmail(email);
        userEntity.setOtpKey(otpKey);
        userEntity.setTelephoneNumber(telephoneNumber);
        userEntity.setDisabled(disabled);
        userEntity.setLoginCounter(loginCounter);
        userEntity.setFeatures(features);
        userEntity.setCreatedAt(createdAt);
        userEntity.setUpdatedAt(updatedAt);
        userEntity.setSessionVersion(sessionVersion);
        userEntity.setSecondFactor(secondFactor);
        userEntity.setProvisionalOtpKey(provisionalOtpKey);
        userEntity.setProvisionalOtpKeyCreatedAt(provisionalOtpKeyCreatedAt);
        userEntity.setLastLoggedInAt(lastLoggedInAt);
        return userEntity;
    }
}
