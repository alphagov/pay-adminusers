package uk.gov.pay.adminusers.persistence.entity;

import javax.persistence.*;
import java.io.Serializable;

@MappedSuperclass
public abstract class AbstractEntity implements Serializable {

    public AbstractEntity() {
        //for jpa
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    @Column(name = "version")
    private Long version;

    public Long getVersion() {
        return version;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
