package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.persistence.entity.GovUkPayAgreementEntity;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.time.LocalDateTime;

public class GovUkPayAgreementDbFixture {

    private final DatabaseTestHelper databaseTestHelper;
    private String email = "someone@exmaple.org";
    private Integer serviceId;
    private LocalDateTime agreementTime = LocalDateTime.now();
    
    private GovUkPayAgreementDbFixture(DatabaseTestHelper databaseTestHelper) {
        this.databaseTestHelper = databaseTestHelper;
    }
    
    public static GovUkPayAgreementDbFixture govUkPayAgreementDbFixture(DatabaseTestHelper databaseTestHelper) {
        return new GovUkPayAgreementDbFixture(databaseTestHelper);
    }
    
    public GovUkPayAgreementEntity insert() {
        GovUkPayAgreementEntity entity = new GovUkPayAgreementEntity(serviceId, email, agreementTime);
        databaseTestHelper.insertGovUkPayAgreementEntity(entity);
        
        return entity;
    }
    
    public GovUkPayAgreementDbFixture withServiceId(Integer serviceId) {
        this.serviceId = serviceId;
        return this;
    }
    
    public GovUkPayAgreementDbFixture withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public GovUkPayAgreementDbFixture withAgreementTime(LocalDateTime agreementTime) {
        this.agreementTime = agreementTime;
        return this;
    }
}
