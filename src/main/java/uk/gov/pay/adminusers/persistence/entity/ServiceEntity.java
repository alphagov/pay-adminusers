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
    private Integer id;

    @OneToMany(mappedBy = "service", targetEntity = GatewayAccountIdEntity.class, fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    private List<GatewayAccountIdEntity> gatewayAccountIds = new ArrayList<>();

    @OneToMany(mappedBy = "service", targetEntity = InviteEntity.class, fetch = FetchType.LAZY)
    private List<InviteEntity> invites = new ArrayList<>();

    public ServiceEntity() {
    }

    public ServiceEntity(List<String> gatewayAccountIds) {
        this.gatewayAccountIds.clear();
        for (String gatewayAccountId : gatewayAccountIds) {
            this.gatewayAccountIds.add(new GatewayAccountIdEntity(gatewayAccountId, this));
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<GatewayAccountIdEntity> getGatewayAccountIds() {
        return ImmutableList.copyOf(this.gatewayAccountIds);
    }

    public GatewayAccountIdEntity getGatewayAccountId() {
        return gatewayAccountIds.get(0);
    }

    public List<InviteEntity> getInvites() {
        return invites;
    }

    public boolean hasExactGatewayAccountIds(List<String> gatewayAccountIds) {

        if (this.gatewayAccountIds.size() != gatewayAccountIds.size()) {
            return false;
        }

        for (GatewayAccountIdEntity gatewayAccountIdEntity : this.gatewayAccountIds) {
            if (!gatewayAccountIds.contains(gatewayAccountIdEntity.getGatewayAccountId())) {
                return false;
            }
        }

        return true;
    }
}
