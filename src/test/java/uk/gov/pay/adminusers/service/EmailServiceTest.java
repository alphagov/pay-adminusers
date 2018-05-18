package uk.gov.pay.adminusers.service;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.pay.adminusers.app.config.NotifyConfiguration;
import uk.gov.pay.adminusers.model.PaymentType;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.resources.EmailTemplate;
import uk.gov.pay.adminusers.resources.InvalidMerchantDetailsException;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class EmailServiceTest {

    private static final String MERCHANT_NAME = "merchant name";
    private static final String TELEPHONE_NUMBER = "call me maybe";
    private static final String ADDRESS_LINE_1 = "address line 1";
    private static final String CITY = "city";
    private static final String POSTCODE = "postcode";
    private static final String ADDRESS_COUNTRY = "cake";
    private static final String MERCHANT_EMAIL = "dd-merchant@example.com";
    private EmailService emailService;

    @Mock
    private NotificationService mockNotificationService;

    @Mock
    private ServiceDao mockServiceDao;

    @Mock
    private NotifyConfiguration mockNotificationConfiguration;

    @Mock
    private ServiceEntity mockServiceEntity;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final static String EMAIL_ADDRESS = "aaa@bbb.test";
    private final static String GATEWAY_ACCOUNT_ID = "DIRECT_DEBIT:sfksdjweg45w";

    @Before
    public void setUp() {
        given(mockNotificationService.getNotifyConfiguration()).willReturn(mockNotificationConfiguration);
        given(mockNotificationConfiguration.getPaymentConfirmedTemplateId()).willReturn("PAYMENT CONFIRMED");
        given(mockNotificationConfiguration.getPaymentFailedTemplateId()).willReturn("PAYMENT FAILED");
        given(mockNotificationConfiguration.getMandateCancelledTemplateId()).willReturn("MANDATE CANCELLED");
        given(mockNotificationConfiguration.getMandateFailedTemplateId()).willReturn("MANDATE FAILED");
        emailService = new EmailService(mockNotificationService, mockServiceDao);
    }

    @Test
    public void shouldSendAnEmailForPaymentConfirmed() throws InvalidMerchantDetailsException {
        EmailTemplate template = EmailTemplate.PAYMENT_CONFIRMED;
        Map<String, String> personalisation = ImmutableMap.of(
                "field 1", "theValueOfField1",
                "field 2", "theValueOfField2"
        );
        MerchantDetailsEntity merchantDetails = new MerchantDetailsEntity(
                MERCHANT_NAME,
                TELEPHONE_NUMBER,
                ADDRESS_LINE_1,
                null,
                CITY,
                POSTCODE,
                ADDRESS_COUNTRY,
                MERCHANT_EMAIL
        );
        given(mockServiceDao.findByGatewayAccountId(GATEWAY_ACCOUNT_ID)).willReturn(Optional.of(mockServiceEntity));
        given(mockServiceEntity.getMerchantDetailsEntity()).willReturn(merchantDetails);
        given(mockServiceEntity.getName()).willReturn("a service");
        ArgumentCaptor<Map<String, String>> personalisationCaptor = forClass(Map.class);

        emailService.sendEmail(EMAIL_ADDRESS, GATEWAY_ACCOUNT_ID, template, personalisation);

        verify(mockNotificationService).sendEmailAsync(eq(PaymentType.DIRECT_DEBIT), eq("PAYMENT CONFIRMED"), eq(EMAIL_ADDRESS), personalisationCaptor.capture());
        Map<String, String> allContent = personalisationCaptor.getValue();
        assertThat(allContent.get("field 1"), is("theValueOfField1"));
        assertThat(allContent.get("field 2"), is("theValueOfField2"));
        assertThat(allContent.get("service name"), is("a service"));
        assertThat(allContent.get("organisation address"), is("merchant name, address line 1, city, postcode, cake"));
        assertThat(allContent.get("organisation phone number"), is(TELEPHONE_NUMBER));
        assertThat(allContent.get("organisation email address"), is(MERCHANT_EMAIL));
    }

    @Test
    public void shouldSendAnEmailForPaymentFailed() throws InvalidMerchantDetailsException {
        EmailTemplate template = EmailTemplate.PAYMENT_FAILED;
        Map<String, String> personalisation = ImmutableMap.of(
                "field 1", "theValueOfField1",
                "field 2", "theValueOfField2"
        );
        MerchantDetailsEntity merchantDetails = new MerchantDetailsEntity(
                MERCHANT_NAME,
                TELEPHONE_NUMBER,
                ADDRESS_LINE_1,
                null,
                CITY,
                POSTCODE,
                ADDRESS_COUNTRY,
                MERCHANT_EMAIL
        );

        given(mockServiceDao.findByGatewayAccountId(GATEWAY_ACCOUNT_ID)).willReturn(Optional.of(mockServiceEntity));
        given(mockServiceEntity.getMerchantDetailsEntity()).willReturn(merchantDetails);
        given(mockServiceEntity.getName()).willReturn("a service");
        ArgumentCaptor<Map<String, String>> personalisationCaptor = forClass(Map.class);

        emailService.sendEmail(EMAIL_ADDRESS, GATEWAY_ACCOUNT_ID, template, personalisation);

        verify(mockNotificationService).sendEmailAsync(eq(PaymentType.DIRECT_DEBIT), eq("PAYMENT FAILED"), eq(EMAIL_ADDRESS), personalisationCaptor.capture());
        Map<String, String> allContent = personalisationCaptor.getValue();
        assertThat(allContent.get("field 1"), is("theValueOfField1"));
        assertThat(allContent.get("field 2"), is("theValueOfField2"));
        assertThat(allContent.get("organisation name"), is(MERCHANT_NAME));
        assertThat(allContent.get("organisation phone number"), is(TELEPHONE_NUMBER));
    }

    @Test
    public void shouldSendAnEmailForMandateFailed() throws InvalidMerchantDetailsException {
        EmailTemplate template = EmailTemplate.MANDATE_FAILED;
        Map<String, String> personalisation = ImmutableMap.of(
                "field 1", "theValueOfField1",
                "field 2", "theValueOfField2"
        );
        MerchantDetailsEntity merchantDetails = new MerchantDetailsEntity(
                MERCHANT_NAME,
                TELEPHONE_NUMBER,
                ADDRESS_LINE_1,
                "address line 2",
                CITY,
                POSTCODE,
                ADDRESS_COUNTRY,
                MERCHANT_EMAIL
        );

        given(mockServiceDao.findByGatewayAccountId(GATEWAY_ACCOUNT_ID)).willReturn(Optional.of(mockServiceEntity));
        given(mockServiceEntity.getMerchantDetailsEntity()).willReturn(merchantDetails);
        given(mockServiceEntity.getName()).willReturn("a service");
        ArgumentCaptor<Map<String, String>> personalisationCaptor = forClass(Map.class);

        emailService.sendEmail(EMAIL_ADDRESS, GATEWAY_ACCOUNT_ID, template, personalisation);

        verify(mockNotificationService).sendEmailAsync(eq(PaymentType.DIRECT_DEBIT), eq("MANDATE FAILED"), eq(EMAIL_ADDRESS), personalisationCaptor.capture());
        Map<String, String> allContent = personalisationCaptor.getValue();
        assertThat(allContent.get("field 1"), is("theValueOfField1"));
        assertThat(allContent.get("field 2"), is("theValueOfField2"));
        assertThat(allContent.get("organisation name"), is(MERCHANT_NAME));
        assertThat(allContent.get("organisation phone number"), is(TELEPHONE_NUMBER));
        assertThat(allContent.get("organisation email address"), is(MERCHANT_EMAIL));
    }

    @Test
    public void shouldThrowAnExceptionIfMerchantDetailsAreMissing() throws InvalidMerchantDetailsException {
        EmailTemplate template = EmailTemplate.MANDATE_FAILED;
        Map<String, String> personalisation = ImmutableMap.of(
                "field 1", "theValueOfField1",
                "field 2", "theValueOfField2"
        );
        MerchantDetailsEntity merchantDetails = new MerchantDetailsEntity(
                null,
                TELEPHONE_NUMBER,
                ADDRESS_LINE_1,
                "address line 2",
                CITY,
                POSTCODE,
                ADDRESS_COUNTRY,
                MERCHANT_EMAIL
        );

        given(mockServiceDao.findByGatewayAccountId(GATEWAY_ACCOUNT_ID)).willReturn(Optional.of(mockServiceEntity));
        given(mockServiceEntity.getMerchantDetailsEntity()).willReturn(merchantDetails);
        given(mockServiceEntity.getName()).willReturn("a service");

        thrown.expect(InvalidMerchantDetailsException.class);
        thrown.expectMessage("Merchant details are missing mandatory fields: can't send email for account " + GATEWAY_ACCOUNT_ID);
        thrown.reportMissingExceptionWithMessage("InvalidMerchantDetailsException expected");

        emailService.sendEmail(EMAIL_ADDRESS, GATEWAY_ACCOUNT_ID, template, personalisation);
    }
}
