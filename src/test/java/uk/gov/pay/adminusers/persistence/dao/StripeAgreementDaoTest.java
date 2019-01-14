package uk.gov.pay.adminusers.persistence.dao;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.entity.StripeAgreementEntity;

import javax.persistence.RollbackException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.StripeAgreementDbFixture.stripeAgreementDbFixture;

public class StripeAgreementDaoTest extends DaoTestBase {

    private StripeAgreementDao stripeAgreementDao;

    @Before
    public void before() {
        stripeAgreementDao = env.getInstance(StripeAgreementDao.class);
    }

    @Test
    public void shouldPersistStripeAgreementEntity() {
        int serviceId = serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(randomInt().toString())
                .insertService()
                .getId();

        StripeAgreementEntity stripeAgreementEntity = new StripeAgreementEntity(serviceId, "192.0.2.0", LocalDateTime.now());
        stripeAgreementDao.persist(stripeAgreementEntity);

        assertThat(stripeAgreementEntity.getId(), is(notNullValue()));
        List<Map<String, Object>> searchResults = databaseHelper.findStripeAgreementById(stripeAgreementEntity.getId());
        assertThat(searchResults.size(), is(1));
        assertThat(searchResults.get(0).get("service_id"), is(stripeAgreementEntity.getServiceId()));
        assertThat(searchResults.get(0).get("ip_address"), is(stripeAgreementEntity.getIpAddress()));
        assertThat(searchResults.get(0).get("id"), is(stripeAgreementEntity.getId()));
        assertThat(((Timestamp) searchResults.get(0).get("agreement_time")).toLocalDateTime(), is(stripeAgreementEntity.getAgreementTime()));
    }

    @Test
    public void shouldNotPersistStripeAgreementEntityWhenOneAlreadyExistsForService() {
        int serviceId = serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(randomInt().toString())
                .insertService()
                .getId();

        StripeAgreementEntity stripeAgreementEntity = new StripeAgreementEntity(serviceId, "192.0.2.0", LocalDateTime.now());
        stripeAgreementDao.persist(stripeAgreementEntity);

        StripeAgreementEntity anotherStripeAgreementEntity = new StripeAgreementEntity(serviceId, "192.0.2.1", LocalDateTime.now());
        try {
            stripeAgreementDao.persist(anotherStripeAgreementEntity);
            fail();
        } catch (RollbackException e) {
            assertTrue(e.getMessage().contains("Key (service_id)=(" + serviceId + ") already exists."));
        }
    }

    @Test
    public void shouldFindStripeAgreementEntityByServiceId() {
        Service service = serviceDbFixture(databaseHelper)
                .insertService();

        StripeAgreementEntity stripeAgreementEntity = stripeAgreementDbFixture(databaseHelper)
                .withServiceId(service.getId())
                .insert();

        Optional<StripeAgreementEntity> maybeStripeAgreementEntity = stripeAgreementDao.findByServiceId(stripeAgreementEntity.getServiceId());
        assertTrue(maybeStripeAgreementEntity.isPresent());
        assertThat(maybeStripeAgreementEntity.get().getIpAddress(), is(stripeAgreementEntity.getIpAddress()));
        assertThat(maybeStripeAgreementEntity.get().getServiceId(), is(stripeAgreementEntity.getServiceId()));
        assertThat(maybeStripeAgreementEntity.get().getAgreementTime(), is(stripeAgreementEntity.getAgreementTime()));
    }

    @Test
    public void shouldNotFindStripeAgreementEntityWhenNoneExistForServiceId() {
        Service service = serviceDbFixture(databaseHelper)
                .insertService();

        stripeAgreementDbFixture(databaseHelper)
                .withServiceId(service.getId())
                .insert();

        Optional<StripeAgreementEntity> maybeStripeAgreementEntity = stripeAgreementDao.findByServiceId(nextInt());
        assertFalse(maybeStripeAgreementEntity.isPresent());
    }
}
