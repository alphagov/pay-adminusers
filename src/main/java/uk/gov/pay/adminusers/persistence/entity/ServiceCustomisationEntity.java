package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.model.ServiceCustomisations;

import javax.persistence.*;
import java.time.ZonedDateTime;

import static java.time.ZonedDateTime.now;

@Entity
@Table(name = "service_customisations")
@SequenceGenerator(name = "service_customisations_seq_gen", sequenceName = "service_customisations_id_seq", allocationSize = 1)
public class ServiceCustomisationEntity extends AbstractEntity {

    @Column(name = "banner_colour")
    private String bannerColour;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "updated")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime updated;

    //for JPA
    public ServiceCustomisationEntity() {
    }

    public ServiceCustomisationEntity(ServiceCustomisations serviceCustomisations) {
        this.bannerColour = serviceCustomisations.getBannerColour();
        this.logoUrl = serviceCustomisations.getLogoUrl();
        this.updated = now();
    }

    public String getBannerColour() {
        return bannerColour;
    }

    public void setBannerColour(String bannerColour) {
        this.bannerColour = bannerColour;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public ZonedDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(ZonedDateTime updated) {
        this.updated = updated;
    }

    public ServiceCustomisations toServiceCustomisations() {
        return new ServiceCustomisations(bannerColour,logoUrl);
    }
}
