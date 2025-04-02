package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.model.ServiceRole;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

@Entity
@Table(name = "user_services_roles")
public class ServiceRoleEntity {

    @EmbeddedId
    private UserServiceId userServiceId;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("serviceId")
    @JoinColumn(name = "service_id", referencedColumnName = "id")
    private ServiceEntity service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", referencedColumnName = "id")
    private RoleEntity role;


    public ServiceRoleEntity() {
    }

    public ServiceRoleEntity(ServiceEntity service, RoleEntity role) {
        this.service = service;
        this.role = role;
    }

    public ServiceEntity getService() {
        return service;
    }

    public void setService(ServiceEntity service) {
        this.service = service;
    }

    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    public UserServiceId getUserServiceId() {
        return userServiceId;
    }

    public void setUserServiceId(UserServiceId userServiceId) {
        this.userServiceId = userServiceId;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity userEntity) {
        this.user = userEntity;
    }

    public ServiceRole toServiceRole() {
        return ServiceRole.from(getService().toService(), getRole().toRole());
    }
}
