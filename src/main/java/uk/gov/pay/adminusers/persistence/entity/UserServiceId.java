package uk.gov.pay.adminusers.persistence.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class UserServiceId implements Serializable {

    @Column(name = "service_id", nullable = false)
    private Integer serviceId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    public Integer getServiceId() {
        return serviceId;
    }

    public void setServiceId(Integer serviceId) {
        this.serviceId = serviceId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}
