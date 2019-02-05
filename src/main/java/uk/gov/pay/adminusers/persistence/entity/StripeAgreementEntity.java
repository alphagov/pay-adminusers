package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.model.StripeAgreement;

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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;

@Entity
@Table(name = "stripe_agreements")
@SequenceGenerator(name = "stripe_agreements_id_seq_gen", sequenceName = "stripe_agreements_id_seq", allocationSize = 1)

public class StripeAgreementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stripe_agreements_id_seq_gen")
    private int id;
    
    @OneToOne
    @JoinColumn(name = "service_id", referencedColumnName = "id")
    private ServiceEntity service;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "agreement_time", columnDefinition = "TIMESTAMP WITH TIME ZONE NOT NULL")
    @Convert(converter = UTCDateTimeConverter.class)
    private ZonedDateTime agreementTime;

    public StripeAgreementEntity() {
        // for jpa
    }

    public StripeAgreementEntity(ServiceEntity service, String ipAddress, ZonedDateTime agreementTime) {
        this.service = service;
        this.ipAddress = ipAddress;
        this.agreementTime = agreementTime;
    }
    
    public StripeAgreement toStripeAgreement() {
        try {
            return new StripeAgreement(InetAddress.getByName(ipAddress), agreementTime);
        } catch (UnknownHostException e) {
            // Ip addresses are validated before storing them in the table so its very unlikely this will happen.
            throw new RuntimeException(String.format("%s is not a valid InetAddress.", ipAddress));
        }
    }

    public int getId() {
        return id;
    }

    public ServiceEntity getService() {
        return service;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public ZonedDateTime getAgreementTime() {
        return agreementTime;
    }
}
