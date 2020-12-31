package uk.gov.pay.adminusers.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "service_gateway_accounts")
@SequenceGenerator(name = "service_gatewayAccounts_seq_gen", sequenceName = "service_gateway_accounts_id_seq", allocationSize = 1)
public class GatewayAccountIdEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "service_gatewayAccounts_seq_gen")
    private Long id;

    @Column(name = "gateway_account_id")
    private String gatewayAccountId;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ServiceEntity service;

    public GatewayAccountIdEntity() {
    }

    public GatewayAccountIdEntity(String gatewayAccountId, ServiceEntity service) {
        this.gatewayAccountId = gatewayAccountId;
        this.service = service;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }

    public void setGatewayAccountId(String gatewayAccountId) {
        this.gatewayAccountId = gatewayAccountId;
    }

    public ServiceEntity getService() {
        return service;
    }

    public void setService(ServiceEntity service) {
        this.service = service;
    }
}
