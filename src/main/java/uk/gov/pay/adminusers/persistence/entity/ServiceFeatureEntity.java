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
@Table(name = "service_features")
@SequenceGenerator(name = "service_features_seq_gen", sequenceName = "service_features_id_seq", allocationSize = 1)
public class ServiceFeatureEntity {
    @Id
    private Long id;
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "service_features_seq_gen")

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ServiceEntity service;
    
    @Column(name = "feature")
    private String feature;

    public ServiceFeatureEntity(ServiceEntity service, String feature) {
        this.service = service;
        this.feature = feature;
    }

    public ServiceFeatureEntity() {
    }
    
    public String getFeature() {
        return feature;
    }
}
