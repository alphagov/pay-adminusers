package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class StripeAgreementDbFixture {

    private final DatabaseTestHelper databaseTestHelper;
    private String ipAddress = "192.0.2.0";
    private int serviceId;
    private ZonedDateTime agreementTime = ZonedDateTime.now(ZoneId.of("UTC"));

    private StripeAgreementDbFixture(DatabaseTestHelper databaseTestHelper) {
        this.databaseTestHelper = databaseTestHelper;
    }

    public static StripeAgreementDbFixture stripeAgreementDbFixture(DatabaseTestHelper databaseTestHelper) {
        return new StripeAgreementDbFixture(databaseTestHelper);
    }

    public void insert() {
        databaseTestHelper.insertStripeAgreementEntity(serviceId, agreementTime, ipAddress);
    }

    public StripeAgreementDbFixture withIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    public StripeAgreementDbFixture withServiceId(int serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public StripeAgreementDbFixture withAgreementTime(ZonedDateTime agreementTime) {
        this.agreementTime = agreementTime;
        return this;
    }
}
