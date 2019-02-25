package uk.gov.pay.adminusers.service;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.app.config.NotifyConfiguration;
import uk.gov.pay.adminusers.app.config.NotifyDirectDebitConfiguration;
import uk.gov.pay.adminusers.model.PaymentType;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.resources.EmailTemplate;
import uk.gov.pay.adminusers.resources.InvalidMerchantDetailsException;
import uk.gov.pay.adminusers.utils.CountryConverter;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class EmailServiceTest {

    private static final String MERCHANT_NAME = "merchant name";
    private static final String TELEPHONE_NUMBER = "call me maybe";
    private static final String ADDRESS_LINE_1 = "address line 1";
    private static final String CITY = "city";
    private static final String POSTCODE = "postcode";
    private static final String ADDRESS_COUNTRY_CODE = "CK";
    private static final String MERCHANT_EMAIL = "dd-merchant@example.com";
    private EmailService emailService;

    @Mock
    private NotificationService mockNotificationService;

    @Mock
    private ServiceDao mockServiceDao;

    @Mock
    private NotifyDirectDebitConfiguration mockNotifyDirectDebitConfiguration;

    @Mock
    private ServiceEntity mockServiceEntity;

    @Mock
    private CountryConverter mockCountryConverter;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final static String EMAIL_ADDRESS = "aaa@bbb.test";
    private final static String GATEWAY_ACCOUNT_ID = "DIRECT_DEBIT:sfksdjweg45w";

    @Before
    public void setUp() {
        given(mockNotificationService.getNotifyDirectDebitConfiguration()).willReturn(mockNotifyDirectDebitConfiguration);
        given(mockNotifyDirectDebitConfiguration.getMandateCancelledEmailTemplateId()).willReturn("NOTIFY_MANDATE_CANCELLED_EMAIL_TEMPLATE_ID_VALUE");
        given(mockNotifyDirectDebitConfiguration.getMandateFailedEmailTemplateId()).willReturn("NOTIFY_MANDATE_FAILED_EMAIL_TEMPLATE_ID_VALUE");
        given(mockNotifyDirectDebitConfiguration.getPaymentFailedEmailTemplateId()).willReturn("NOTIFY_PAYMENT_FAILED_EMAIL_TEMPLATE_ID_VALUE");
        given(mockNotifyDirectDebitConfiguration.getOneOffMandateAndPaymentCreatedEmailTemplateId()).willReturn("NOTIFY_ONE_OFF_MANDATE_AND_PAYMENT_CREATED_EMAIL_TEMPLATE_ID_VALUE");
        given(mockNotifyDirectDebitConfiguration.getOnDemandMandateCreatedEmailTemplateId()).willReturn("NOTIFY_ON_DEMAND_MANDATE_CREATED_EMAIL_TEMPLATE_ID_VALUE");
        given(mockNotifyDirectDebitConfiguration.getOnDemandPaymentConfirmedEmailTemplateId()).willReturn("NOTIFY_ON_DEMAND_PAYMENT_CONFIRMED_EMAIL_TEMPLATE_ID_VALUE");
        given(mockServiceDao.findByGatewayAccountId(GATEWAY_ACCOUNT_ID)).willReturn(Optional.of(mockServiceEntity));
        given(mockServiceEntity.getName()).willReturn("a service");
        given(mockCountryConverter.getCountryNameFrom(ADDRESS_COUNTRY_CODE)).willReturn(Optional.of("Cake Land"));
        emailService = new EmailService(mockNotificationService, mockCountryConverter, mockServiceDao);
    }

    @Test
    public void shouldSendAnEmailForOneOffPaymentConfirmed() throws InvalidMerchantDetailsException {
        EmailTemplate template = EmailTemplate.ONE_OFF_PAYMENT_CONFIRMED;
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
                ADDRESS_COUNTRY_CODE,
                MERCHANT_EMAIL
        );
        given(mockServiceEntity.getMerchantDetailsEntity()).willReturn(merchantDetails);
        ArgumentCaptor<Map<String, String>> personalisationCaptor = forClass(Map.class);
        emailService.sendEmail(EMAIL_ADDRESS, GATEWAY_ACCOUNT_ID, template, personalisation);

        verify(mockNotificationService).sendEmailAsync(eq(PaymentType.DIRECT_DEBIT), eq("NOTIFY_ONE_OFF_MANDATE_AND_PAYMENT_CREATED_EMAIL_TEMPLATE_ID_VALUE"), eq(EMAIL_ADDRESS), personalisationCaptor.capture());
        Map<String, String> allContent = personalisationCaptor.getValue();
        assertThat(allContent.get("field 1"), is("theValueOfField1"));
        assertThat(allContent.get("field 2"), is("theValueOfField2"));
        assertThat(allContent.get("service name"), is("a service"));
        assertThat(allContent.get("organisation name"), is(MERCHANT_NAME));
        assertThat(allContent.get("organisation address"), is("address line 1, city, postcode, Cake Land"));
        assertThat(allContent.get("organisation phone number"), is(TELEPHONE_NUMBER));
        assertThat(allContent.get("organisation email address"), is(MERCHANT_EMAIL));
    }

    @Test
    public void shouldSendAnEmailForOnDemandPaymentConfirmed() throws InvalidMerchantDetailsException {
        EmailTemplate template = EmailTemplate.ON_DEMAND_PAYMENT_CONFIRMED;
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
                ADDRESS_COUNTRY_CODE,
                MERCHANT_EMAIL
        );
        given(mockServiceEntity.getMerchantDetailsEntity()).willReturn(merchantDetails);
        ArgumentCaptor<Map<String, String>> personalisationCaptor = forClass(Map.class);
        emailService.sendEmail(EMAIL_ADDRESS, GATEWAY_ACCOUNT_ID, template, personalisation);

        verify(mockNotificationService).sendEmailAsync(eq(PaymentType.DIRECT_DEBIT), eq("NOTIFY_ON_DEMAND_PAYMENT_CONFIRMED_EMAIL_TEMPLATE_ID_VALUE"), eq(EMAIL_ADDRESS), personalisationCaptor.capture());
        Map<String, String> allContent = personalisationCaptor.getValue();
        assertThat(allContent.get("field 1"), is("theValueOfField1"));
        assertThat(allContent.get("field 2"), is("theValueOfField2"));
        assertThat(allContent.get("service name"), is("a service"));
        assertThat(allContent.get("organisation name"), is(MERCHANT_NAME));
        assertThat(allContent.get("organisation address"), is("address line 1, city, postcode, Cake Land"));
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
                ADDRESS_COUNTRY_CODE,
                MERCHANT_EMAIL
        );
        given(mockServiceEntity.getMerchantDetailsEntity()).willReturn(merchantDetails);
        ArgumentCaptor<Map<String, String>> personalisationCaptor = forClass(Map.class);

        emailService.sendEmail(EMAIL_ADDRESS, GATEWAY_ACCOUNT_ID, template, personalisation);

        verify(mockNotificationService).sendEmailAsync(eq(PaymentType.DIRECT_DEBIT), eq("NOTIFY_PAYMENT_FAILED_EMAIL_TEMPLATE_ID_VALUE"), eq(EMAIL_ADDRESS), personalisationCaptor.capture());
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
                ADDRESS_COUNTRY_CODE,
                MERCHANT_EMAIL
        );

        given(mockServiceEntity.getMerchantDetailsEntity()).willReturn(merchantDetails);
        ArgumentCaptor<Map<String, String>> personalisationCaptor = forClass(Map.class);

        emailService.sendEmail(EMAIL_ADDRESS, GATEWAY_ACCOUNT_ID, template, personalisation);

        verify(mockNotificationService).sendEmailAsync(eq(PaymentType.DIRECT_DEBIT), eq("NOTIFY_MANDATE_FAILED_EMAIL_TEMPLATE_ID_VALUE"), eq(EMAIL_ADDRESS), personalisationCaptor.capture());
        Map<String, String> allContent = personalisationCaptor.getValue();
        assertThat(allContent.get("field 1"), is("theValueOfField1"));
        assertThat(allContent.get("field 2"), is("theValueOfField2"));
        assertThat(allContent.get("organisation name"), is(MERCHANT_NAME));
        assertThat(allContent.get("organisation phone number"), is(TELEPHONE_NUMBER));
        assertThat(allContent.get("organisation email address"), is(MERCHANT_EMAIL));
    }

    @Test
    public void shouldSendAnEmailForOnDemandMandateCreated() throws InvalidMerchantDetailsException {
        EmailTemplate template = EmailTemplate.ON_DEMAND_MANDATE_CREATED;
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
                ADDRESS_COUNTRY_CODE,
                MERCHANT_EMAIL
        );

        given(mockServiceEntity.getMerchantDetailsEntity()).willReturn(merchantDetails);
        ArgumentCaptor<Map<String, String>> personalisationCaptor = forClass(Map.class);

        emailService.sendEmail(EMAIL_ADDRESS, GATEWAY_ACCOUNT_ID, template, personalisation);

        verify(mockNotificationService).sendEmailAsync(eq(PaymentType.DIRECT_DEBIT), eq("NOTIFY_ON_DEMAND_MANDATE_CREATED_EMAIL_TEMPLATE_ID_VALUE"), eq(EMAIL_ADDRESS), personalisationCaptor.capture());
        Map<String, String> allContent = personalisationCaptor.getValue();
        assertThat(allContent.get("field 1"), is("theValueOfField1"));
        assertThat(allContent.get("field 2"), is("theValueOfField2"));
        assertThat(allContent.get("organisation name"), is(MERCHANT_NAME));
        assertThat(allContent.get("organisation phone number"), is(TELEPHONE_NUMBER));
        assertThat(allContent.get("organisation email address"), is(MERCHANT_EMAIL));
    }

    @Test
    public void shouldSendAnEmailForOneOffMandateCreated() throws InvalidMerchantDetailsException {
        EmailTemplate template = EmailTemplate.ONE_OFF_MANDATE_CREATED;
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
                ADDRESS_COUNTRY_CODE,
                MERCHANT_EMAIL
        );

        given(mockServiceEntity.getMerchantDetailsEntity()).willReturn(merchantDetails);
        ArgumentCaptor<Map<String, String>> personalisationCaptor = forClass(Map.class);

        emailService.sendEmail(EMAIL_ADDRESS, GATEWAY_ACCOUNT_ID, template, personalisation);

        verify(mockNotificationService).sendEmailAsync(eq(PaymentType.DIRECT_DEBIT), eq("NOTIFY_ONE_OFF_MANDATE_AND_PAYMENT_CREATED_EMAIL_TEMPLATE_ID_VALUE"), eq(EMAIL_ADDRESS), personalisationCaptor.capture());
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
                ADDRESS_COUNTRY_CODE,
                MERCHANT_EMAIL
        );

        given(mockServiceEntity.getMerchantDetailsEntity()).willReturn(merchantDetails);
        thrown.expect(InvalidMerchantDetailsException.class);
        thrown.expectMessage("Merchant details are missing mandatory fields: can't send email for account " + GATEWAY_ACCOUNT_ID);
        thrown.reportMissingExceptionWithMessage("InvalidMerchantDetailsException expected");

        emailService.sendEmail(EMAIL_ADDRESS, GATEWAY_ACCOUNT_ID, template, personalisation);
    }

    @Test
    public void shouldNotDisplayCountryNameForInvalidCountryCode() throws InvalidMerchantDetailsException {
        EmailTemplate template = EmailTemplate.ONE_OFF_PAYMENT_CONFIRMED;
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
                ADDRESS_COUNTRY_CODE,
                MERCHANT_EMAIL
        );
        given(mockServiceEntity.getMerchantDetailsEntity()).willReturn(merchantDetails);
        given(mockCountryConverter.getCountryNameFrom(ADDRESS_COUNTRY_CODE)).willReturn(Optional.empty());
        ArgumentCaptor<Map<String, String>> personalisationCaptor = forClass(Map.class);
        emailService.sendEmail(EMAIL_ADDRESS, GATEWAY_ACCOUNT_ID, template, personalisation);

        verify(mockNotificationService).sendEmailAsync(eq(PaymentType.DIRECT_DEBIT), eq("NOTIFY_ONE_OFF_MANDATE_AND_PAYMENT_CREATED_EMAIL_TEMPLATE_ID_VALUE"), eq(EMAIL_ADDRESS), personalisationCaptor.capture());
        Map<String, String> allContent = personalisationCaptor.getValue();
        assertThat(allContent.get("field 1"), is("theValueOfField1"));
        assertThat(allContent.get("field 2"), is("theValueOfField2"));
        assertThat(allContent.get("service name"), is("a service"));
        assertThat(allContent.get("organisation name"), is(MERCHANT_NAME));
        assertThat(allContent.get("organisation address"), is("address line 1, city, postcode"));
        assertThat(allContent.get("organisation phone number"), is(TELEPHONE_NUMBER));
        assertThat(allContent.get("organisation email address"), is(MERCHANT_EMAIL));
    }
}
