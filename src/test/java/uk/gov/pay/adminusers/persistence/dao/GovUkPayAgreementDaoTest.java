package uk.gov.pay.adminusers.persistence.dao;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.entity.GovUkPayAgreementEntity;

import javax.persistence.RollbackException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.fixtures.GovUkPayAgreementDbFixture.govUkPayAgreementDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;

public class GovUkPayAgreementDaoTest extends DaoTestBase{
    
    private GovUkPayAgreementDao agreementDao;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Before
    public void setUp() {
        agreementDao = env.getInstance(GovUkPayAgreementDao.class);
    }
    
    @Test
    public void shouldPersistEntity() {
        int serviceId = serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(randomInt().toString())
                .insertService()
                .getId();

        GovUkPayAgreementEntity entity = new GovUkPayAgreementEntity(serviceId, 
                "someone@example.org", 
                LocalDateTime.now());
        
        agreementDao.persist(entity);
        assertThat(entity.getId(), is(notNullValue()));
        List<Map<String, Object>> searchResults = databaseHelper.findGovUkPayAgreementEntity(entity.getId());
        assertThat(searchResults.size(), is(1));
        assertThat(searchResults.get(0).get("service_id"), is(serviceId));
        assertThat(searchResults.get(0).get("email"), is("someone@example.org"));
        assertThat(((Timestamp) searchResults.get(0).get("agreement_time")).toLocalDateTime(), is(entity.getAgreementTime()));
    }

    @Test
    public void shouldNotPersistStripeAgreementEntityWhenOneAlreadyExistsForService() {
        int serviceId = serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(randomInt().toString())
                .insertService()
                .getId();

        GovUkPayAgreementEntity entity = new GovUkPayAgreementEntity(serviceId,
                "someone@example.org",
                LocalDateTime.now());
        agreementDao.persist(entity);

        GovUkPayAgreementEntity anotherEntity = new GovUkPayAgreementEntity(serviceId,
                "someone.else@example.org",
                LocalDateTime.now());
        expectedException.expect(RollbackException.class);
        expectedException.expectMessage("Key (service_id)=(" + serviceId + ") already exists.");
        agreementDao.persist(anotherEntity);
    }

    @Test
    public void shouldNotFindStripeAgreementEntityWhenNoneExistForServiceId() {
        Service service = serviceDbFixture(databaseHelper)
                .insertService();
        
        govUkPayAgreementDbFixture(databaseHelper)
                .withServiceId(service.getId())
                .insert();
        Optional<GovUkPayAgreementEntity> maybeEntity = agreementDao.findByServiceId(service.getId() + 1);
        assertThat(maybeEntity.isPresent(), is(false));
    }
}
