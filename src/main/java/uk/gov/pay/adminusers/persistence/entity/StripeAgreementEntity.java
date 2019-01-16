package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.model.StripeAgreement;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "stripe_agreements")
@SequenceGenerator(name = "stripe_agreements_id_seq_gen", sequenceName = "stripe_agreements_id_seq", allocationSize = 1)

public class StripeAgreementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stripe_agreements_id_seq_gen")
    private int id;

    @Column(name = "service_id")
    private int serviceId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "agreement_time", columnDefinition = "TIMESTAMP WITH TIME ZONE NOT NULL")
    private LocalDateTime agreementTime;

    public StripeAgreementEntity() {
        // for jpa
    }

    public StripeAgreementEntity(int serviceId, String ipAddress, LocalDateTime agreementTime) {
        this.serviceId = serviceId;
        this.ipAddress = ipAddress;
        this.agreementTime = agreementTime;
    }
    
    public static StripeAgreementEntity from(StripeAgreement stripeAgreement) {
        return new StripeAgreementEntity(stripeAgreement.getServiceId(), stripeAgreement.getIpAddress(), stripeAgreement.getAgreementTime());
    }
    
    public StripeAgreement toStripeAgreement() {
        return new StripeAgreement(serviceId, ipAddress, agreementTime);
    }

    public int getId() {
        return id;
    }

    public int getServiceId() {
        return serviceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public LocalDateTime getAgreementTime() {
        return agreementTime;
    }
}
