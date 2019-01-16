package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.model.GovUkPayAgreement;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
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



