package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.entity.GovUkPayAgreementEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class GovUkPayAgreementDbFixture {

    private final DatabaseTestHelper databaseTestHelper;
    private String email = "someone@exmaple.org";
    private ZonedDateTime agreementTime = ZonedDateTime.now(ZoneOffset.UTC);
    private ServiceEntity serviceEntity;
    
    private GovUkPayAgreementDbFixture(DatabaseTestHelper databaseTestHelper) {
        this.databaseTestHelper = databaseTestHelper;
    }
    
    public static GovUkPayAgreementDbFixture govUkPayAgreementDbFixture(DatabaseTestHelper databaseTestHelper) {
        return new GovUkPayAgreementDbFixture(databaseTestHelper);
    }
    
    public GovUkPayAgreementEntity insert() {
        if (serviceEntity == null) {
            serviceEntity = ServiceEntity.from(ServiceDbFixture.serviceDbFixture(databaseTestHelper).insertService());
        }
        GovUkPayAgreementEntity entity = new GovUkPayAgreementEntity(email, agreementTime);
        entity.setService(serviceEntity);
        databaseTestHelper.insertGovUkPayAgreementEntity(entity);
        
        return entity;
    }
    
    public GovUkPayAgreementDbFixture withServiceEntity(ServiceEntity serviceEntity) {
        this.serviceEntity = serviceEntity;
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
