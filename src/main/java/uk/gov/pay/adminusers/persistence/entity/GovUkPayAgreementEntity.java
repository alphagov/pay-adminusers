package uk.gov.pay.adminusers.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import uk.gov.pay.adminusers.model.GovUkPayAgreement;

import java.time.ZonedDateTime;

@Entity
@Table(name = "govuk_pay_agreements")
public class GovUkPayAgreementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "govuk_pay_agreements_id_seq_gen")
    @SequenceGenerator(name = "govuk_pay_agreements_id_seq_gen", sequenceName = "govuk_pay_agreements_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "email")
    private String email;

    @Column(name = "agreement_time", columnDefinition = "TIMESTAMP WITH TIME ZONE NOT NULL")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime agreementTime;

    @OneToOne
    @JoinColumn(name = "service_id")
    private ServiceEntity service;

    public GovUkPayAgreementEntity(){
        // for jpa
    }

    public GovUkPayAgreementEntity(String email, ZonedDateTime agreementTime) {
        this.email = email;
        this.agreementTime = agreementTime;
    }
    
    public GovUkPayAgreement toGovUkPayAgreement() {
        return new GovUkPayAgreement(email, agreementTime);
    }

    public Integer getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public ZonedDateTime getAgreementTime() {
        return agreementTime;
    }

    public ServiceEntity getService() {
        return service;
    }

    public void setService(ServiceEntity service) {
        this.service = service;
    }
}



