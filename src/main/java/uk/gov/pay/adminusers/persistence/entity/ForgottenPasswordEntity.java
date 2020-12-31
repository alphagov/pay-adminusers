package uk.gov.pay.adminusers.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import uk.gov.pay.adminusers.model.ForgottenPassword;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static uk.gov.pay.adminusers.model.ForgottenPassword.forgottenPassword;

@Entity
@Table(name = "forgotten_passwords")
@SequenceGenerator(name = "forgotten_passwords_id_seq", sequenceName = "forgotten_passwords_id_seq", allocationSize = 1)
public class ForgottenPasswordEntity extends AbstractEntity {

    @Column(name = "date")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime date;

    @Column(name = "code")
    private String code;

    /**
     * stupid sequalize created a column with with uppercase "I", hence the double quotes
     * TODO: rename this once we are completely migrated out of sequalize
     */
    @ManyToOne
    @JoinColumn(name = "\"userId\"", updatable = false)
    private UserEntity user;

    // TODO: Change column from 'camelCase' to 'snake_case'. These columns were created through Sequelize.
    @Column(name = "\"createdAt\"")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime createdAt;

    @Column(name = "\"updatedAt\"")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime updatedAt;

    /**
     * For JPA
     */
    public ForgottenPasswordEntity() {
        super();
    }

    public ForgottenPasswordEntity(String code, ZonedDateTime date, UserEntity user) {
        super();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        this.date = date == null ? now : date;
        this.code = code;
        this.user = user;
        this.createdAt = now;
        this.updatedAt = now;
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

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
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

    public static ForgottenPasswordEntity from(ForgottenPassword forgottenPassword, UserEntity user) {
        return new ForgottenPasswordEntity(forgottenPassword.getCode(), forgottenPassword.getDate(), user);
    }

    public ForgottenPassword toForgottenPassword() {
        return forgottenPassword(getId(), code, date, user.getExternalId());
    }
}
