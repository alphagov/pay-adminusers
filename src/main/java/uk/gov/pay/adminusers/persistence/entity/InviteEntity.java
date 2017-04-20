package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.model.Invite;

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

    @ManyToOne
    @JoinColumn(name = "service_id")
    private ServiceEntity service;

    @Column(name = "email")
    private String email;

    @Column(name = "code")
    private String code;

    public InviteEntity() {
        //for jpa
    }

    public InviteEntity(String email, String code, ServiceEntity service, RoleEntity role) {
        this.service = service;
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

    public Invite toInvite() {
        return new Invite(email, code);
    }
}
