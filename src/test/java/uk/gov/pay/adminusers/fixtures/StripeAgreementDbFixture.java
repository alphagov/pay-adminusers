package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.persistence.entity.StripeAgreementEntity;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.time.LocalDateTime;

public class StripeAgreementDbFixture {

    private final DatabaseTestHelper databaseTestHelper;
    private String ipAddress = "192.0.2.0";
    private int serviceId;
    private LocalDateTime agreementTime = LocalDateTime.now();

    private StripeAgreementDbFixture(DatabaseTestHelper databaseTestHelper) {
        this.databaseTestHelper = databaseTestHelper;
    }

    public static StripeAgreementDbFixture stripeAgreementDbFixture(DatabaseTestHelper databaseTestHelper) {
        return new StripeAgreementDbFixture(databaseTestHelper);
    }

    public StripeAgreementEntity insert() {
        StripeAgreementEntity stripeAgreementEntity = new StripeAgreementEntity(serviceId, ipAddress, agreementTime);
        databaseTestHelper.insertStripeAgreementEntity(stripeAgreementEntity);
        return stripeAgreementEntity;
    }

    public StripeAgreementDbFixture withIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    public StripeAgreementDbFixture withServiceId(int serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public StripeAgreementDbFixture withAgreementTime(LocalDateTime agreementTime) {
        this.agreementTime = agreementTime;
        return this;
    }
}
