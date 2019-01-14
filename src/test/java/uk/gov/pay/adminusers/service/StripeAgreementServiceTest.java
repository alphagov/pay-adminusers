package uk.gov.pay.adminusers.service;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.model.StripeAgreement;
import uk.gov.pay.adminusers.model.StripeAgreementRequest;
import uk.gov.pay.adminusers.persistence.dao.StripeAgreementDao;
import uk.gov.pay.adminusers.persistence.entity.StripeAgreementEntity;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)

public class StripeAgreementServiceTest {
    
    @Mock
    StripeAgreementDao mockedStripeAgreementDao;

    @Captor
    ArgumentCaptor<StripeAgreementEntity> stripeAgreementEntityArgumentCaptor;

    private StripeAgreementService stripeAgreementService;

    @Before
    public void before() {
        stripeAgreementService = new StripeAgreementService(mockedStripeAgreementDao);
    }

    @Test
    public void shouldReturnStripeAgreement() {
        int serviceId = 1;
        String ipAddress = "192.0.2.0";
        LocalDateTime agreementTime = LocalDateTime.now();
        
        StripeAgreementEntity stripeAgreementEntity = 
                new StripeAgreementEntity(serviceId, ipAddress, agreementTime);
        
        when(mockedStripeAgreementDao.findByServiceId(serviceId))
                .thenReturn(Optional.of(stripeAgreementEntity));

        Optional<StripeAgreement> maybeStripeAgreement = stripeAgreementService
                .findStripeAgreementByServiceId(serviceId);
        
        assertTrue(maybeStripeAgreement.isPresent());
        assertThat(maybeStripeAgreement.get().getIpAddress(), is(ipAddress));
        assertThat(maybeStripeAgreement.get().getAgreementTime(), is(agreementTime));
        assertThat(maybeStripeAgreement.get().getServiceId(), is(serviceId));
    }
    
    @Test
    public void shouldCreateNewStripeAgreement() {
        int serviceId = 1;
        StripeAgreementRequest stripeAgreementRequest = new StripeAgreementRequest("192.0.2.0");
        LocalDateTime agreementTime = LocalDateTime.now();

        stripeAgreementService.doCreate(serviceId, stripeAgreementRequest, agreementTime);
        
        verify(mockedStripeAgreementDao, times(1))
                .persist(stripeAgreementEntityArgumentCaptor.capture());
        
        assertThat(stripeAgreementEntityArgumentCaptor.getValue().getIpAddress(), is(stripeAgreementRequest.getIpAddress()));
        assertThat(stripeAgreementEntityArgumentCaptor.getValue().getServiceId(), is(serviceId));
        assertThat(stripeAgreementEntityArgumentCaptor.getValue().getAgreementTime(), is(agreementTime));
    }
}
