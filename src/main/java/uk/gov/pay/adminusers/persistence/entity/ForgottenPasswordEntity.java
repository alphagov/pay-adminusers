package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.model.ForgottenPassword;

import javax.persistence.*;
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

    public ForgottenPasswordEntity() {
        //for jpa
    }

    public ForgottenPasswordEntity(String code, ZonedDateTime date, UserEntity user) {
        this.date = date == null ? ZonedDateTime.now(ZoneId.of("UTC")) : date;
        this.code = code;
        this.user = user;
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

    public static ForgottenPasswordEntity from(ForgottenPassword forgottenPassword, UserEntity user) {
        return new ForgottenPasswordEntity(forgottenPassword.getCode(), forgottenPassword.getDate(), user);
    }

    public ForgottenPassword toForgottenPassword() {
        return forgottenPassword(getId(), code, user.getUsername(), date);
    }
}
