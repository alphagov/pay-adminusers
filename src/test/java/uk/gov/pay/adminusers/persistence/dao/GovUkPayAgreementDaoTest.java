package uk.gov.pay.adminusers.persistence.dao;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.entity.GovUkPayAgreementEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import javax.persistence.RollbackException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.fixtures.GovUkPayAgreementDbFixture.govUkPayAgreementDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;

public class GovUkPayAgreementDaoTest extends DaoTestBase{
    
    private GovUkPayAgreementDao agreementDao;
    private ServiceDao serviceDao;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Before
    public void setUp() {
        agreementDao = env.getInstance(GovUkPayAgreementDao.class);
        serviceDao = env.getInstance(ServiceDao.class);
    }
    
    @Test
    public void shouldPersistEntity() {
        Service service = serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(randomInt().toString())
                .insertService();
        ZonedDateTime dateTime = ZonedDateTime.now(ZoneOffset.UTC);
        GovUkPayAgreementEntity newEntity = new GovUkPayAgreementEntity("someone@example.org", dateTime);
        Optional<ServiceEntity> serviceEntity = serviceDao.findByExternalId(service.getExternalId());
        newEntity.setService(serviceEntity.get());
        
        agreementDao.persist(newEntity);
        List<Map<String, Object>> searchResults = databaseHelper.findGovUkPayAgreementEntity(serviceEntity.get().getId());
        assertThat(searchResults.size(), is(1));
        assertThat(searchResults.get(0).get("service_id"), is(service.getId()));
        assertThat(searchResults.get(0).get("email"), is("someone@example.org"));
        assertThat(searchResults.get(0).get("agreement_time"), is(java.sql.Timestamp.from(dateTime.toInstant())));
    }

    @Test
    public void shouldNotPersistGovUkPayAgreementEntityWhenOneAlreadyExistsForService() {
        Service service = serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(randomInt().toString())
                .insertService();
        ZonedDateTime dateTime = ZonedDateTime.now(ZoneOffset.UTC);
        GovUkPayAgreementEntity newEntity = new GovUkPayAgreementEntity("someone@example.org", dateTime);
        Optional<ServiceEntity> serviceEntity = serviceDao.findByExternalId(service.getExternalId());
        newEntity.setService(serviceEntity.get());

        agreementDao.persist(newEntity);

        GovUkPayAgreementEntity anotherEntity = new GovUkPayAgreementEntity("someone.else@example.org", ZonedDateTime.now());
        anotherEntity.setService(serviceEntity.get());
        expectedException.expect(RollbackException.class);
        expectedException.expectMessage("Key (service_id)=(" + serviceEntity.get().getId() + ") already exists.");
        agreementDao.persist(anotherEntity);
    }

    @Test
    public void shouldNotFindStripeAgreementEntityWhenNoneExistForServiceId() {
        Service service = serviceDbFixture(databaseHelper)
                .withExternalId("abcde1234")
                .insertService();
        Optional<ServiceEntity> serviceEntity = serviceDao.findByExternalId(service.getExternalId());
        govUkPayAgreementDbFixture(databaseHelper)
                .withServiceEntity(serviceEntity.get())
                .insert();
        Optional<GovUkPayAgreementEntity> maybeEntity = agreementDao.findByExternalServiceId("abcd1235");
        assertThat(maybeEntity.isPresent(), is(false));
    }
}
