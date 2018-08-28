package uk.gov.pay.adminusers.persistence.entity.service;

import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.commons.model.SupportedLanguage;
import uk.gov.pay.commons.model.SupportedLanguageJpaConverter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "service_names")
public class ServiceNameEntity {

    @Id
    @SequenceGenerator(name = "service_names_id_seq", sequenceName = "service_names_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "service_names_id_seq")
    private Long id;

    @JoinColumn(name = "service_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private ServiceEntity service;

    @Column(name = "language")
    @Enumerated(EnumType.STRING)
    @Convert(converter = SupportedLanguageJpaConverter.class)
    private SupportedLanguage language;

    @Column(name = "name")
    private String name;

    public ServiceNameEntity() {
        // make JPA happy
    }

    public static ServiceNameEntity from(SupportedLanguage language, String name) {
        ServiceNameEntity entity = new ServiceNameEntity();
        entity.setLanguage(language);
        entity.setName(name);

        return entity;
    }

    //region <Getters/Setters>
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServiceEntity getService() {
        return service;
    }

    public void setService(ServiceEntity service) {
        this.service = service;
    }

    public SupportedLanguage getLanguage() {
        return language;
    }

    public void setLanguage(SupportedLanguage language) {
        this.language = language;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceNameEntity that = (ServiceNameEntity) o;
        return Objects.equals(service, that.service) &&
                Objects.equals(language, that.language) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, language, name);
    }

    //endregion
}
