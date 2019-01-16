package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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


@RunWith(MockitoJUnitRunner.class)
public class GovUkPayAgreementServiceTest {

    @Mock
    private GovUkPayAgreementDao mockedAgreementDao;
    
    @Captor
    private ArgumentCaptor<GovUkPayAgreementEntity> argumentCaptor;
    
    private GovUkPayAgreementService agreementService;
    @Before
    public void setUp() throws Exception {
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
        
        agreementService.doCreate(serviceEntity, email, ZonedDateTime.now());
        
        verify(mockedAgreementDao).persist(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getEmail(), is(email));
    }
}
