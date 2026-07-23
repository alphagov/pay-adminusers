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

import java.util.Objects;

@Entity
@Table(name = "service_features")
@SequenceGenerator(name = "service_features_seq_gen", sequenceName = "service_features_id_seq", allocationSize = 1)
public class ServiceFeatureEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "services_seq_gen")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ServiceFeatureEntity that = (ServiceFeatureEntity) o;
        return Objects.equals(service, that.service) && Objects.equals(feature, that.feature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, feature);
    }
}
