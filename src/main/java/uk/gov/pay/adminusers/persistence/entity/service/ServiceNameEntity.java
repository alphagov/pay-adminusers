package uk.gov.pay.adminusers.persistence.entity.service;

import uk.gov.pay.adminusers.model.ServiceUpdateRequest;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

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
import java.util.Map;
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
    @Convert(converter = SupportedLanguageConverter.class)
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

    public static ServiceNameEntity from(ServiceUpdateRequest updateRequest) {
        ServiceNameEntity entity = new ServiceNameEntity();
        Map<String, Object> stringObjectMap = updateRequest.valueAsObject();

        if (!stringObjectMap.isEmpty()) {
            final String languageCode = stringObjectMap.keySet().toArray()[0].toString();
            final String name = stringObjectMap.values().toArray()[0].toString();
            entity.setLanguage(SupportedLanguage.fromIso639AlphaTwoCode(languageCode));
            entity.setName(name);
        }

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
                Objects.equals(language, that.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, language);
    }

    //endregion
}
