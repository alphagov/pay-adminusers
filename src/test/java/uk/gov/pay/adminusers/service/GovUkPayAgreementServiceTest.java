package uk.gov.pay.adminusers.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.model.GovUkPayAgreement;
import uk.gov.pay.adminusers.persistence.dao.GovUkPayAgreementDao;
import uk.gov.pay.adminusers.persistence.entity.GovUkPayAgreementEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class GovUkPayAgreementServiceTest {

    @Mock
    private GovUkPayAgreementDao mockedAgreementDao;
    
    @Captor
    private ArgumentCaptor<GovUkPayAgreementEntity> argumentCaptor;
    
    private GovUkPayAgreementService agreementService;
    @BeforeEach
    public void setUp() {
        agreementService = new GovUkPayAgreementService(mockedAgreementDao);
    }
    
    @Test
    public void shouldReturnGovUkPayAgreement() {
        String serviceExternalId = "abcd1234";
        String email = "someone@example.com";
        ZonedDateTime agreementTime = ZonedDateTime.now(ZoneOffset.UTC);
        
        GovUkPayAgreementEntity entity = new GovUkPayAgreementEntity(email, agreementTime);
        when(mockedAgreementDao.findByExternalServiceId(serviceExternalId))
                .thenReturn(Optional.of(entity));
        
        Optional<GovUkPayAgreement> optionalGovUkPayAgreement = 
                agreementService.findGovUkPayAgreementByServiceId(serviceExternalId);
        
        assertThat(optionalGovUkPayAgreement.isPresent(), is(true));
        assertThat(optionalGovUkPayAgreement.get().getEmail(), is(email));
        assertThat(optionalGovUkPayAgreement.get().getAgreementTime(), is(agreementTime));
    }
    
    @Test
    public void shouldCreateNewGovUkPayAgreement() {
        ServiceEntity serviceEntity = new ServiceEntity();
        String email = "someone@example.com";
        ZonedDateTime now = ZonedDateTime.now();
        
        GovUkPayAgreement agreement = agreementService.doCreate(serviceEntity, email, now);
        
        verify(mockedAgreementDao).persist(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getEmail(), is(email));
        assertThat(agreement.getEmail(), is(email));
        assertThat(agreement.getAgreementTime(), is(now));
    }
}
