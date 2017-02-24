package uk.gov.pay.adminusers.persistence.entity;

import com.google.common.collect.ImmutableList;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "services")
@SequenceGenerator(name = "services_seq_gen", sequenceName = "services_id_seq", allocationSize = 1)
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "services_seq_gen")
    private Long id;

    @OneToMany(mappedBy = "service", targetEntity = GatewayAccountEntity.class, fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    private List<GatewayAccountEntity> gatewayAccounts = new ArrayList<>();

    public ServiceEntity() {
    }

    public ServiceEntity(String gatewayAccountId) {
        this.gatewayAccounts.clear();
        this.gatewayAccounts.add(new GatewayAccountEntity(gatewayAccountId, this));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<GatewayAccountEntity> getGatewayAccounts() {
        return ImmutableList.copyOf(this.gatewayAccounts);
    }

    public GatewayAccountEntity getGatewayAccount() {
        return gatewayAccounts.get(0);
    }
}
