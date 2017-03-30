package uk.gov.pay.adminusers.persistence.entity;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.ZonedDateTime.*;

@Entity
@Table(name = "invites")
public class InviteEntity extends AbstractEntity {

    @Column(name = "date")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime date;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private RoleEntity role;

    @Column(name = "email")
    private String email;

    @Column(name = "code")
    private String code;

    public InviteEntity() {
        //for jpa
    }

    public InviteEntity(String email, String code, RoleEntity role) {
        this.date = now(ZoneId.of("UTC"));
        this.code = code;
        this.email = email;
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

    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
