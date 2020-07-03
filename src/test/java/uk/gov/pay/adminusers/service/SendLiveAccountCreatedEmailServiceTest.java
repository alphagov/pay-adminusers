package uk.gov.pay.adminusers.service;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.app.config.LinksConfig;
import uk.gov.pay.adminusers.exception.GovUkPayAgreementNotSignedException;
import uk.gov.pay.adminusers.persistence.dao.GovUkPayAgreementDao;
import uk.gov.pay.adminusers.persistence.entity.GovUkPayAgreementEntity;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SendLiveAccountCreatedEmailServiceTest {
    
    @Mock
    private GovUkPayAgreementDao mockGovUkPayAgreementDao;
    @Mock
    private AdminUsersConfig mockConfig;
    @Mock
    private NotificationService mockNotificationService;

    private static final String SELFSERVICE_SERVICES_URL = "http://selfservice/services";
    
    private SendLiveAccountCreatedEmailService sendLiveAccountCreatedEmailService;

    @Before
    public void setUp() {
        LinksConfig mockLinks = mock(LinksConfig.class);
        when(mockLinks.getSelfserviceServicesUrl()).thenReturn(SELFSERVICE_SERVICES_URL);
        when(mockConfig.getLinks()).thenReturn(mockLinks);
        sendLiveAccountCreatedEmailService = new SendLiveAccountCreatedEmailService(mockGovUkPayAgreementDao, mockNotificationService, mockConfig);
    }

    @Test
    public void shouldSendServiceIsLiveEmail_whenAgreementIsSigned() {
        String serviceExternalId = "abc123";
        String email = "some-user@example.com";
        
        GovUkPayAgreementEntity mockAgreement = mock(GovUkPayAgreementEntity.class);
        when(mockAgreement.getEmail()).thenReturn(email);
        when(mockGovUkPayAgreementDao.findByExternalServiceId(serviceExternalId)).thenReturn(Optional.of(mockAgreement));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        when(mockNotificationService.sendLiveAccountCreatedEmail(eq(email), urlCaptor.capture()))
                .thenReturn("random-notify-id");
        
        sendLiveAccountCreatedEmailService.sendEmail(serviceExternalId);

        assertThat(urlCaptor.getValue(), is(SELFSERVICE_SERVICES_URL + '/' + serviceExternalId + "/live-account"));
    }

    @Test
    public void shouldThrowException_whenAgreementNotSigned() {
        String serviceExternalId = "abc123";
        
        when(mockGovUkPayAgreementDao.findByExternalServiceId(serviceExternalId)).thenReturn(Optional.empty());

        GovUkPayAgreementNotSignedException exception = assertThrows(GovUkPayAgreementNotSignedException.class,
                () -> sendLiveAccountCreatedEmailService.sendEmail(serviceExternalId));
        assertThat(exception.getMessage(), is("Nobody from this service is on record as having agreed to the legal terms"));
    }
}
