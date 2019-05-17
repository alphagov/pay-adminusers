package uk.gov.pay.adminusers.persistence.dao;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.StripeAgreementEntity;

import javax.persistence.RollbackException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.StripeAgreementDbFixture.stripeAgreementDbFixture;

public class StripeAgreementDaoIT extends DaoTestBase {

    private StripeAgreementDao stripeAgreementDao;
    private ServiceDao serviceDao;

    @Before
    public void before() {
        stripeAgreementDao = env.getInstance(StripeAgreementDao.class);
        serviceDao = env.getInstance(ServiceDao.class);
    }

    @Test
    public void shouldPersistStripeAgreementEntity() {
        Service service = serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(randomInt().toString())
                .insertService();
        
        Optional<ServiceEntity> serviceEntity = serviceDao.findByExternalId(service.getExternalId());
        assertThat(serviceEntity.isPresent(), is(true));

        StripeAgreementEntity stripeAgreementEntity = new StripeAgreementEntity(serviceEntity.get(), "192.0.2.0", ZonedDateTime.now(ZoneId.of("UTC")));
        stripeAgreementDao.persist(stripeAgreementEntity);

        assertThat(stripeAgreementEntity.getId(), is(notNullValue()));
        List<Map<String, Object>> searchResults = databaseHelper.findStripeAgreementById(stripeAgreementEntity.getId());
        assertThat(searchResults.size(), is(1));
        assertThat(searchResults.get(0).get("service_id"), is(stripeAgreementEntity.getService().getId()));
        assertThat(searchResults.get(0).get("ip_address"), is(stripeAgreementEntity.getIpAddress()));
        assertThat(searchResults.get(0).get("id"), is(stripeAgreementEntity.getId()));
        assertThat(searchResults.get(0).get("agreement_time"), is(Timestamp.from(stripeAgreementEntity.getAgreementTime().toInstant())));
    }

    @Test
    public void shouldNotPersistStripeAgreementEntityWhenOneAlreadyExistsForService() {
        Service service = serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(randomInt().toString())
                .insertService();

        Optional<ServiceEntity> serviceEntity = serviceDao.findByExternalId(service.getExternalId());
        assertThat(serviceEntity.isPresent(), is(true));

        StripeAgreementEntity stripeAgreementEntity = new StripeAgreementEntity(serviceEntity.get(), "192.0.2.0", ZonedDateTime.now(ZoneId.of("UTC")));
        stripeAgreementDao.persist(stripeAgreementEntity);

        StripeAgreementEntity anotherStripeAgreementEntity = new StripeAgreementEntity(serviceEntity.get(), "192.0.2.1", ZonedDateTime.now(ZoneId.of("UTC")));
        try {
            stripeAgreementDao.persist(anotherStripeAgreementEntity);
            fail();
        } catch (RollbackException e) {
            assertTrue(e.getMessage().contains("Key (service_id)=(" + serviceEntity.get().getId() + ") already exists."));
        }
    }

    @Test
    public void shouldFindStripeAgreementEntityByServiceId() {
        Service service = serviceDbFixture(databaseHelper)
                .insertService();

        ZonedDateTime agreementTime = ZonedDateTime.now(ZoneId.of("UTC"));
        String ipAddress = "192.0.2.0";
        
        stripeAgreementDbFixture(databaseHelper)
                .withAgreementTime(agreementTime)
                .withIpAddress(ipAddress)
                .withServiceId(service.getId())
                .insert();

        Optional<StripeAgreementEntity> maybeStripeAgreementEntity = stripeAgreementDao.findByServiceExternalId(service.getExternalId());
        assertTrue(maybeStripeAgreementEntity.isPresent());
        assertThat(maybeStripeAgreementEntity.get().getIpAddress(), is(ipAddress));
        assertThat(maybeStripeAgreementEntity.get().getService().getId(), is(service.getId()));
        assertThat(maybeStripeAgreementEntity.get().getAgreementTime(), is(agreementTime));
    }

    @Test
    public void shouldNotFindStripeAgreementEntityWhenNoneExistForServiceId() {
        Service service = serviceDbFixture(databaseHelper)
                .insertService();
        
        stripeAgreementDbFixture(databaseHelper)
                .withServiceId(service.getId())
                .insert();

        Optional<StripeAgreementEntity> maybeStripeAgreementEntity = stripeAgreementDao.findByServiceExternalId("123");
        assertFalse(maybeStripeAgreementEntity.isPresent());
    }
}
