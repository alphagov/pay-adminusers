package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.model.GovUkPayAgreement;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "govuk_pay_agreements")
@SequenceGenerator(name = "govuk_pay_agreements_id_seq_gen", sequenceName = "govuk_pay_agreements_id_seq", allocationSize = 1)
public class GovUkPayAgreementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "govuk_pay_agreements_id_seq_gen")
    private Integer id;

    @Column(name = "service_id")
    private Integer serviceId;

    @Column(name = "email")
    private String email;

    @Column(name = "agreement_time", columnDefinition = "TIMESTAMP WITH TIME ZONE NOT NULL")
    private LocalDateTime agreementTime;
    
    public GovUkPayAgreementEntity(){
        // for jpa
    }
    
    public GovUkPayAgreementEntity(Integer serviceId, String email, LocalDateTime agreementTime) {
        this.serviceId = serviceId;
        this.email = email;
        this.agreementTime = agreementTime;
    }
    
    public static GovUkPayAgreementEntity from(GovUkPayAgreement payAgreement)  {
        return new GovUkPayAgreementEntity(payAgreement.getServiceId(), payAgreement.getEmail(), payAgreement.getAgreementTime());
    }
    
    public GovUkPayAgreement toGovUkPayAgreement() {
        return new GovUkPayAgreement(serviceId, email, agreementTime);
    }

    public Integer getId() {
        return id;
    }

    public Integer getServiceId() {
        return serviceId;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getAgreementTime() {
        return agreementTime;
    }
}
