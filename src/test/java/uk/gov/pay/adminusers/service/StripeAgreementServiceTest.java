package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.exception.ServiceNotFoundException;
import uk.gov.pay.adminusers.exception.StripeAgreementExistsException;
import uk.gov.pay.adminusers.model.StripeAgreement;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.StripeAgreementDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.StripeAgreementEntity;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StripeAgreementServiceTest {
    
    @Mock
    private StripeAgreementDao mockedStripeAgreementDao;
    
    @Mock
    private ServiceDao mockedServiceDao;

    @Captor
    private ArgumentCaptor<StripeAgreementEntity> stripeAgreementEntityArgumentCaptor;

    private StripeAgreementService stripeAgreementService;

    @BeforeEach
    public void before() {
        stripeAgreementService = new StripeAgreementService(mockedStripeAgreementDao, mockedServiceDao);
    }

    @Test
    public void shouldReturnStripeAgreement() {
        String serviceExternalId = "abc123";
        String ipAddress = "192.0.2.0";
        ZonedDateTime agreementTime = ZonedDateTime.now();

        ServiceEntity mockServiceEntity = mock(ServiceEntity.class);
        
        StripeAgreementEntity stripeAgreementEntity = 
                new StripeAgreementEntity(mockServiceEntity, ipAddress, agreementTime);
        
        when(mockedStripeAgreementDao.findByServiceExternalId(serviceExternalId))
                .thenReturn(Optional.of(stripeAgreementEntity));

        Optional<StripeAgreement> maybeStripeAgreement = stripeAgreementService
                .findStripeAgreementByServiceId(serviceExternalId);
        
        assertTrue(maybeStripeAgreement.isPresent());
        assertThat(maybeStripeAgreement.get().getIpAddress().getHostAddress(), is(ipAddress));
        assertThat(maybeStripeAgreement.get().getAgreementTime(), is(agreementTime));
    }
    
    @Test
    public void shouldCreateNewStripeAgreement() throws UnknownHostException {
        String serviceExternalId = "abc123";
        String ipAddress = "192.0.2.0";

        ServiceEntity mockServiceEntity = mock(ServiceEntity.class);
        when(mockedServiceDao.findByExternalId(serviceExternalId)).thenReturn(Optional.of(mockServiceEntity));
        
        stripeAgreementService.doCreate(serviceExternalId, InetAddress.getByName(ipAddress));
        
        verify(mockedStripeAgreementDao, times(1))
                .persist(stripeAgreementEntityArgumentCaptor.capture());
        
        assertThat(stripeAgreementEntityArgumentCaptor.getValue().getIpAddress(), is(ipAddress));
        assertThat(stripeAgreementEntityArgumentCaptor.getValue().getService(), is(mockServiceEntity));
    }

    @Test
    public void shouldThrowException_whenServiceDoesNotExist() {
        String serviceExternalId = "abc123";
        when(mockedServiceDao.findByExternalId(serviceExternalId)).thenReturn(Optional.empty());

        assertThrows(ServiceNotFoundException.class,
                () -> stripeAgreementService.doCreate(serviceExternalId, InetAddress.getByName("192.0.2.0")));
    }

    @Test
    public void shouldThrowException_whenStripeAgreementAlreadyExists() throws UnknownHostException {
        String serviceExternalId = "abc123";

        ServiceEntity mockServiceEntity = mock(ServiceEntity.class);
        when(mockedServiceDao.findByExternalId(serviceExternalId)).thenReturn(Optional.of(mockServiceEntity));

        StripeAgreementEntity mockStripeAgreementEntity = mock(StripeAgreementEntity.class);
        when(mockedStripeAgreementDao.findByServiceExternalId(serviceExternalId)).thenReturn(Optional.of(mockStripeAgreementEntity));

        StripeAgreementExistsException exception = assertThrows(StripeAgreementExistsException.class,
                () -> stripeAgreementService.doCreate(serviceExternalId, InetAddress.getByName("192.0.2.0")));

        assertThat(exception.getMessage(),
                is("Stripe agreement information is already stored for this service"));
    }
}
