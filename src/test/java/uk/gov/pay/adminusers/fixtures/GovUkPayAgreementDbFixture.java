package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class GovUkPayAgreementDbFixture {

    private final DatabaseTestHelper databaseTestHelper;
    private String email = "someone@exmaple.org";
    private ZonedDateTime agreementTime = ZonedDateTime.now(ZoneOffset.UTC);
    private int serviceId;
    
    private GovUkPayAgreementDbFixture(DatabaseTestHelper databaseTestHelper) {
        this.databaseTestHelper = databaseTestHelper;
    }
    
    public static GovUkPayAgreementDbFixture govUkPayAgreementDbFixture(DatabaseTestHelper databaseTestHelper) {
        return new GovUkPayAgreementDbFixture(databaseTestHelper);
    }
    
    public void insert() {
        databaseTestHelper.insertGovUkPayAgreementEntity(serviceId, email, agreementTime);
    }
    
    public GovUkPayAgreementDbFixture withServiceId(int serviceId) {
        this.serviceId = serviceId;
        return this;
    }
    
    public GovUkPayAgreementDbFixture withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public GovUkPayAgreementDbFixture withAgreementTime(ZonedDateTime agreementTime) {
        this.agreementTime = agreementTime;
        return this;
    }
}
