package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.app.config.NotifyDirectDebitConfiguration;
import uk.gov.pay.adminusers.model.PaymentType;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.pay.adminusers.resources.EmailTemplate;
import uk.gov.pay.adminusers.resources.InvalidMerchantDetailsException;
import uk.gov.pay.adminusers.utils.CountryConverter;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.service.payments.commons.model.SupportedLanguage.ENGLISH;

@ExtendWith(MockitoExtension.class)
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

    private final static String EMAIL_ADDRESS = "aaa@bbb.test";
    private final static String GATEWAY_ACCOUNT_ID = "DIRECT_DEBIT:sfksdjweg45w";

    @BeforeEach
    public void setUp() {
        given(mockNotificationService.getNotifyDirectDebitConfiguration()).willReturn(mockNotifyDirectDebitConfiguration);
        given(mockNotifyDirectDebitConfiguration.getMandateCancelledEmailTemplateId()).willReturn("NOTIFY_MANDATE_CANCELLED_EMAIL_TEMPLATE_ID_VALUE");
        given(mockNotifyDirectDebitConfiguration.getMandateFailedEmailTemplateId()).willReturn("NOTIFY_MANDATE_FAILED_EMAIL_TEMPLATE_ID_VALUE");
        given(mockNotifyDirectDebitConfiguration.getPaymentFailedEmailTemplateId()).willReturn("NOTIFY_PAYMENT_FAILED_EMAIL_TEMPLATE_ID_VALUE");
        given(mockNotifyDirectDebitConfiguration.getOneOffMandateAndPaymentCreatedEmailTemplateId()).willReturn("NOTIFY_ONE_OFF_MANDATE_AND_PAYMENT_CREATED_EMAIL_TEMPLATE_ID_VALUE");
        given(mockNotifyDirectDebitConfiguration.getOnDemandMandateCreatedEmailTemplateId()).willReturn("NOTIFY_ON_DEMAND_MANDATE_CREATED_EMAIL_TEMPLATE_ID_VALUE");
        given(mockNotifyDirectDebitConfiguration.getOnDemandPaymentConfirmedEmailTemplateId()).willReturn("NOTIFY_ON_DEMAND_PAYMENT_CONFIRMED_EMAIL_TEMPLATE_ID_VALUE");
        given(mockServiceDao.findByGatewayAccountId(GATEWAY_ACCOUNT_ID)).willReturn(Optional.of(mockServiceEntity));
        given(mockServiceEntity.getServiceNames()).willReturn(Map.of(ENGLISH, ServiceNameEntity.from(ENGLISH, "a service")));
        given(mockCountryConverter.getCountryNameFrom(ADDRESS_COUNTRY_CODE)).willReturn(Optional.of("Cake Land"));
        emailService = new EmailService(mockNotificationService, mockCountryConverter, mockServiceDao);

    }

    @Test
    public void shouldSendAnEmailForOneOffPaymentConfirmed() throws InvalidMerchantDetailsException {
        EmailTemplate template = EmailTemplate.ONE_OFF_PAYMENT_CONFIRMED;
        Map<String, String> personalisation = Map.of(
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

        verify(mockNotificationService).sendEmail(eq(PaymentType.DIRECT_DEBIT), eq("NOTIFY_ONE_OFF_MANDATE_AND_PAYMENT_CREATED_EMAIL_TEMPLATE_ID_VALUE"), eq(EMAIL_ADDRESS), personalisationCaptor.capture());
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
        Map<String, String> personalisation = Map.of(
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

        verify(mockNotificationService).sendEmail(eq(PaymentType.DIRECT_DEBIT), eq("NOTIFY_ON_DEMAND_PAYMENT_CONFIRMED_EMAIL_TEMPLATE_ID_VALUE"), eq(EMAIL_ADDRESS), personalisationCaptor.capture());
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
        Map<String, String> personalisation = Map.of(
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

        verify(mockNotificationService).sendEmail(eq(PaymentType.DIRECT_DEBIT), eq("NOTIFY_PAYMENT_FAILED_EMAIL_TEMPLATE_ID_VALUE"), eq(EMAIL_ADDRESS), personalisationCaptor.capture());
        Map<String, String> allContent = personalisationCaptor.getValue();
        assertThat(allContent.get("field 1"), is("theValueOfField1"));
        assertThat(allContent.get("field 2"), is("theValueOfField2"));
        assertThat(allContent.get("organisation name"), is(MERCHANT_NAME));
        assertThat(allContent.get("organisation phone number"), is(TELEPHONE_NUMBER));
    }

    @Test
    public void shouldSendAnEmailForMandateFailed() throws InvalidMerchantDetailsException {
        EmailTemplate template = EmailTemplate.MANDATE_FAILED;
        Map<String, String> personalisation = Map.of(
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

        verify(mockNotificationService).sendEmail(eq(PaymentType.DIRECT_DEBIT), eq("NOTIFY_MANDATE_FAILED_EMAIL_TEMPLATE_ID_VALUE"), eq(EMAIL_ADDRESS), personalisationCaptor.capture());
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
        Map<String, String> personalisation = Map.of(
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

        verify(mockNotificationService).sendEmail(eq(PaymentType.DIRECT_DEBIT), eq("NOTIFY_ON_DEMAND_MANDATE_CREATED_EMAIL_TEMPLATE_ID_VALUE"), eq(EMAIL_ADDRESS), personalisationCaptor.capture());
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
        Map<String, String> personalisation = Map.of(
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

        verify(mockNotificationService).sendEmail(eq(PaymentType.DIRECT_DEBIT), eq("NOTIFY_ONE_OFF_MANDATE_AND_PAYMENT_CREATED_EMAIL_TEMPLATE_ID_VALUE"), eq(EMAIL_ADDRESS), personalisationCaptor.capture());
        Map<String, String> allContent = personalisationCaptor.getValue();
        assertThat(allContent.get("field 1"), is("theValueOfField1"));
        assertThat(allContent.get("field 2"), is("theValueOfField2"));
        assertThat(allContent.get("organisation name"), is(MERCHANT_NAME));
        assertThat(allContent.get("organisation phone number"), is(TELEPHONE_NUMBER));
        assertThat(allContent.get("organisation email address"), is(MERCHANT_EMAIL));
    }

    @Test
    public void shouldThrowAnExceptionIfMerchantDetailsAreMissing() {
        // reset mocks as these are not used here and Mockito can continue enforcing strict stubs
        Mockito.reset(mockNotificationService, mockNotifyDirectDebitConfiguration, mockServiceEntity, mockCountryConverter);
        EmailTemplate template = EmailTemplate.MANDATE_FAILED;
        Map<String, String> personalisation = Map.of(
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

        InvalidMerchantDetailsException exception = assertThrows(InvalidMerchantDetailsException.class,
                () -> emailService.sendEmail(EMAIL_ADDRESS, GATEWAY_ACCOUNT_ID, template, personalisation));

        assertThat(exception.getMessage(), is("Merchant details are missing mandatory fields: can't send email for account " + GATEWAY_ACCOUNT_ID));
    }

    @Test
    public void shouldNotDisplayCountryNameForInvalidCountryCode() throws InvalidMerchantDetailsException {
        EmailTemplate template = EmailTemplate.ONE_OFF_PAYMENT_CONFIRMED;
        Map<String, String> personalisation = Map.of(
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

        verify(mockNotificationService).sendEmail(eq(PaymentType.DIRECT_DEBIT), eq("NOTIFY_ONE_OFF_MANDATE_AND_PAYMENT_CREATED_EMAIL_TEMPLATE_ID_VALUE"), eq(EMAIL_ADDRESS), personalisationCaptor.capture());
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
