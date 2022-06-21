package uk.gov.pay.adminusers.service;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.app.config.NotifyConfiguration;
import uk.gov.pay.adminusers.app.config.NotifyDirectDebitConfiguration;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {
    
    private static final String OTP = "123456";
    private static final String PHONE_NUMBER = "07700900000";
    private static final String PHONE_NUMBER_E164 = "+447700900000";
    private static final UUID NOTIFICATION_ID = UUID.fromString("0E56AABE-E026-4478-8B82-C03D3B31CFC1");

    private static final String SECOND_FACTOR_SMS_TEMPLATE_ID = "second-factor-sms-template-id";
    private static final String SIGN_IN_OTP_SMS_TEMPLATE_ID = "sign-in-otp-sms-template-id";
    private static final String CHANGE_SIGN_IN_2FA_TO_SMS_OTP_SMS_TEMPLATE_ID = "change-sign-in-2fa-to-sms-otp-sms-template-id";
    private static final String SELF_INITIATED_CREATE_USER_AND_SERVICE_OTP_SMS_TEMPLATE_ID = "self-initiated-create-user-and-service-otp-sms-template-id";
    private static final String CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE_OTP_SMS_TEMPLATE_ID
            = "create-user-in-response-to-invitation-to-service-otp-sms-template-id";
    
    private static final String INVITE_USER_EMAIL_TEMPLATE_ID = "invite-user-email-template-id";
    private static final String INVITE_USER_EXISTING_EMAIL_TEMPLATE_ID = "invite-user-existing-email-template-id";
    private static final String FORGOTTEN_PASSWORD_EMAIL_TEMPLATE_ID = "forgotten-password-email-template-id";
    
    private static final String STRIPE_DISPUTE_CREATED_EMAIL_TEMPLATE_ID = "stripe-dispute-created-email-template-id";
    private static final String STRIPE_DISPUTE_LOST_EMAIL_TEMPLATE_ID = "stripe-dispute-lost-email-template-id";
    private static final String STRIPE_DISPUTE_EVIDENCE_SUBMITTED_EMAIL_TEMPLATE_ID = "stripe-dispute-evidence-submitted-email-template-id";
    private static final String STRIPE_DISPUTE_WON_EMAIL_TEMPLATE_ID = "stripe-dispute-won-email-template-id";
    private static final String NOTIFY_EMAIL_REPLY_TO_SUPPORT_ID = "notify-email-reply-to-support-id";
    
    @Mock private NotifyClientProvider mockNotifyClientProvider;
    @Mock private NotifyConfiguration mockNotifyConfiguration;
    @Mock private NotifyDirectDebitConfiguration mockNotifyDirectDebitConfiguration;
    @Mock private MetricRegistry mockMetricRegistry;

    @Mock private NotificationClient mockNotificationClient;
    @Mock private SendSmsResponse mockSendSmsResponse;
    @Mock private SendEmailResponse mockSendEmailResponse;

    private NotificationService notificationService;

    @BeforeEach
    public void setUp() throws NotificationClientException {
        given(mockNotifyConfiguration.getSignInOtpSmsTemplateId()).willReturn(SIGN_IN_OTP_SMS_TEMPLATE_ID);
        given(mockNotifyConfiguration.getChangeSignIn2faToSmsOtpSmsTemplateId()).willReturn(CHANGE_SIGN_IN_2FA_TO_SMS_OTP_SMS_TEMPLATE_ID);
        given(mockNotifyConfiguration.getSelfInitiatedCreateUserAndServiceOtpSmsTemplateId())
                .willReturn(SELF_INITIATED_CREATE_USER_AND_SERVICE_OTP_SMS_TEMPLATE_ID);
        given(mockNotifyConfiguration.getCreateUserInResponseToInvitationToServiceOtpSmsTemplateId())
                .willReturn(CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE_OTP_SMS_TEMPLATE_ID);

        given(mockNotifyConfiguration.getInviteUserEmailTemplateId()).willReturn(INVITE_USER_EMAIL_TEMPLATE_ID);
        given(mockNotifyConfiguration.getInviteUserExistingEmailTemplateId()).willReturn(INVITE_USER_EXISTING_EMAIL_TEMPLATE_ID);
        given(mockNotifyConfiguration.getForgottenPasswordEmailTemplateId()).willReturn(FORGOTTEN_PASSWORD_EMAIL_TEMPLATE_ID);
        
        given(mockNotifyConfiguration.getStripeDisputeCreatedEmailTemplateId()).willReturn(STRIPE_DISPUTE_CREATED_EMAIL_TEMPLATE_ID);
        given(mockNotifyConfiguration.getStripeDisputeLostEmailTemplateId()).willReturn(STRIPE_DISPUTE_LOST_EMAIL_TEMPLATE_ID);
        given(mockNotifyConfiguration.getStripeDisputeEvidenceSubmittedEmailTemplateId()).willReturn(STRIPE_DISPUTE_EVIDENCE_SUBMITTED_EMAIL_TEMPLATE_ID);
        given(mockNotifyConfiguration.getStripeDisputeWonEmailTemplateId()).willReturn(STRIPE_DISPUTE_WON_EMAIL_TEMPLATE_ID);
        given(mockNotifyConfiguration.getNotifyEmailReplyToSupportId()).willReturn(NOTIFY_EMAIL_REPLY_TO_SUPPORT_ID);
        
        given(mockNotifyClientProvider.get()).willReturn(mockNotificationClient);
        

        notificationService = new NotificationService(mockNotifyClientProvider, mockNotifyConfiguration, mockNotifyDirectDebitConfiguration,
                mockMetricRegistry);
    }

    @Test
    public void sendSecondFactorPasscodeSmsWithSignInTemplate() throws NotificationClientException {
        given(mockMetricRegistry.histogram("notify-operations.sms.response_time")).willReturn(mock(Histogram.class));
        given(mockNotificationClient.sendSms(anyString(), anyString(), anyMap(), isNull())).willReturn(mockSendSmsResponse);
        given(mockSendSmsResponse.getNotificationId()).willReturn(NOTIFICATION_ID);
        
        notificationService.sendSecondFactorPasscodeSms(PHONE_NUMBER, OTP, OtpNotifySmsTemplateId.SIGN_IN);

        verify(mockNotificationClient).sendSms(SIGN_IN_OTP_SMS_TEMPLATE_ID, PHONE_NUMBER_E164, Map.of("code", OTP), null);
    }

    @Test
    public void sendSecondFactorPasscodeSmsWithChangeSignIn2faToSmsTemplate() throws NotificationClientException {
        given(mockMetricRegistry.histogram("notify-operations.sms.response_time")).willReturn(mock(Histogram.class));
        given(mockNotificationClient.sendSms(anyString(), anyString(), anyMap(), isNull())).willReturn(mockSendSmsResponse);
        given(mockSendSmsResponse.getNotificationId()).willReturn(NOTIFICATION_ID);
        
        notificationService.sendSecondFactorPasscodeSms(PHONE_NUMBER, OTP, OtpNotifySmsTemplateId.CHANGE_SIGN_IN_2FA_TO_SMS);

        verify(mockNotificationClient).sendSms(CHANGE_SIGN_IN_2FA_TO_SMS_OTP_SMS_TEMPLATE_ID, PHONE_NUMBER_E164, Map.of("code", OTP), null);
    }

    @Test
    public void sendSecondFactorPasscodeSmsWithSelfInitiatedCreateNewUserAndServiceTemplate() throws NotificationClientException {
        given(mockMetricRegistry.histogram("notify-operations.sms.response_time")).willReturn(mock(Histogram.class));
        given(mockNotificationClient.sendSms(anyString(), anyString(), anyMap(), isNull())).willReturn(mockSendSmsResponse);
        given(mockSendSmsResponse.getNotificationId()).willReturn(NOTIFICATION_ID);
        
        notificationService.sendSecondFactorPasscodeSms(PHONE_NUMBER, OTP, OtpNotifySmsTemplateId.SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE);

        verify(mockNotificationClient).sendSms(SELF_INITIATED_CREATE_USER_AND_SERVICE_OTP_SMS_TEMPLATE_ID, PHONE_NUMBER_E164, Map.of("code", OTP),
                null);    
    }

    @Test
    public void sendSecondFactorPasscodeSmsWithCreateUserInResponseToInvitationToServiceTemplate() throws NotificationClientException {
        given(mockMetricRegistry.histogram("notify-operations.sms.response_time")).willReturn(mock(Histogram.class));
        given(mockNotificationClient.sendSms(anyString(), anyString(), anyMap(), isNull())).willReturn(mockSendSmsResponse);
        given(mockSendSmsResponse.getNotificationId()).willReturn(NOTIFICATION_ID);
        
        notificationService.sendSecondFactorPasscodeSms(PHONE_NUMBER, OTP, OtpNotifySmsTemplateId.CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE);

        verify(mockNotificationClient).sendSms(CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE_OTP_SMS_TEMPLATE_ID, PHONE_NUMBER_E164, Map.of("code", OTP),
                null);    
    }
    
    @Test
    public void sendEmailWithStripeDisputeCreatedEmailTemplateId() throws NotificationClientException {
        given(mockMetricRegistry.histogram("notify-operations.email.response_time")).willReturn(mock(Histogram.class));
        given(mockNotificationClient.sendEmail(anyString(), anyString(), anyMap(), isNull(), anyString())).willReturn(mockSendEmailResponse);
        given(mockSendEmailResponse.getNotificationId()).willReturn(NOTIFICATION_ID);
        
        var addresses = Stream.of("email1@service.gov.uk", "email2@service.gov.uk")
                .collect(Collectors.toSet());
        var personalisation = Stream.of(new String[][] {
                { "k1", "v1" },
                { "k2", "v2" }
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
        
        notificationService.sendStripeDisputeCreatedEmail(addresses, personalisation);
        
        verify(mockNotificationClient).sendEmail(STRIPE_DISPUTE_CREATED_EMAIL_TEMPLATE_ID, "email1@service.gov.uk", personalisation, null, NOTIFY_EMAIL_REPLY_TO_SUPPORT_ID);
        verify(mockNotificationClient).sendEmail(STRIPE_DISPUTE_CREATED_EMAIL_TEMPLATE_ID, "email2@service.gov.uk", personalisation, null, NOTIFY_EMAIL_REPLY_TO_SUPPORT_ID);
    }

    @Test
    public void sendEmailWithStripeDisputeLostEmailTemplateId() throws NotificationClientException {
        given(mockMetricRegistry.histogram("notify-operations.email.response_time")).willReturn(mock(Histogram.class));
        given(mockNotificationClient.sendEmail(anyString(), anyString(), anyMap(), isNull(), anyString())).willReturn(mockSendEmailResponse);
        given(mockSendEmailResponse.getNotificationId()).willReturn(NOTIFICATION_ID);

        var addresses = Stream.of("email1@service.gov.uk", "email2@service.gov.uk")
                .collect(Collectors.toSet());
        var personalisation = Stream.of(new String[][] {
                { "k1", "v1" },
                { "k2", "v2" }
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

        notificationService.sendStripeDisputeLostEmail(addresses, personalisation);

        verify(mockNotificationClient).sendEmail(STRIPE_DISPUTE_LOST_EMAIL_TEMPLATE_ID, "email1@service.gov.uk", personalisation, null, NOTIFY_EMAIL_REPLY_TO_SUPPORT_ID);
        verify(mockNotificationClient).sendEmail(STRIPE_DISPUTE_LOST_EMAIL_TEMPLATE_ID, "email2@service.gov.uk", personalisation, null, NOTIFY_EMAIL_REPLY_TO_SUPPORT_ID);
    }

    @Test
    public void sendEmailWithStripeDisputeEvidenceSubmittedEmailTemplateId() throws NotificationClientException {
        given(mockMetricRegistry.histogram("notify-operations.email.response_time")).willReturn(mock(Histogram.class));
        given(mockNotificationClient.sendEmail(anyString(), anyString(), anyMap(), isNull(), anyString())).willReturn(mockSendEmailResponse);
        given(mockSendEmailResponse.getNotificationId()).willReturn(NOTIFICATION_ID);

        var addresses = Stream.of("email1@service.gov.uk", "email2@service.gov.uk")
                .collect(Collectors.toSet());
        var personalisation = Stream.of(new String[][] {
                { "k1", "v1" },
                { "k2", "v2" }
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

        notificationService.sendStripeDisputeEvidenceSubmittedEmail(addresses, personalisation);

        verify(mockNotificationClient).sendEmail(STRIPE_DISPUTE_EVIDENCE_SUBMITTED_EMAIL_TEMPLATE_ID, "email1@service.gov.uk", personalisation, null, NOTIFY_EMAIL_REPLY_TO_SUPPORT_ID);
        verify(mockNotificationClient).sendEmail(STRIPE_DISPUTE_EVIDENCE_SUBMITTED_EMAIL_TEMPLATE_ID, "email2@service.gov.uk", personalisation, null, NOTIFY_EMAIL_REPLY_TO_SUPPORT_ID);
    }

    @Test
    public void sendEmailWithStripeDisputeWonEmailTemplateId() throws NotificationClientException {
        given(mockMetricRegistry.histogram("notify-operations.email.response_time")).willReturn(mock(Histogram.class));
        given(mockNotificationClient.sendEmail(anyString(), anyString(), anyMap(), isNull(), anyString())).willReturn(mockSendEmailResponse);
        given(mockSendEmailResponse.getNotificationId()).willReturn(NOTIFICATION_ID);

        var addresses = Stream.of("email1@service.gov.uk", "email2@service.gov.uk")
                .collect(Collectors.toSet());
        var personalisation = Stream.of(new String[][] {
                { "k1", "v1" },
                { "k2", "v2" }
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

        notificationService.sendStripeDisputeWonEmail(addresses, personalisation);

        verify(mockNotificationClient).sendEmail(STRIPE_DISPUTE_WON_EMAIL_TEMPLATE_ID, "email1@service.gov.uk", personalisation, null, NOTIFY_EMAIL_REPLY_TO_SUPPORT_ID);
        verify(mockNotificationClient).sendEmail(STRIPE_DISPUTE_WON_EMAIL_TEMPLATE_ID, "email2@service.gov.uk", personalisation, null, NOTIFY_EMAIL_REPLY_TO_SUPPORT_ID);
    }
}
