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
import uk.gov.service.notify.SendSmsResponse;

import java.util.Map;
import java.util.UUID;

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
    
    @Mock private NotifyClientProvider mockNotifyClientProvider;
    @Mock private NotifyConfiguration mockNotifyConfiguration;
    @Mock private NotifyDirectDebitConfiguration mockNotifyDirectDebitConfiguration;
    @Mock private MetricRegistry mockMetricRegistry;

    @Mock private NotificationClient mockNotificationClient;
    @Mock private SendSmsResponse mockSendSmsResponse;

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
        
        given(mockNotifyClientProvider.get()).willReturn(mockNotificationClient);

        given(mockMetricRegistry.histogram("notify-operations.sms.response_time")).willReturn(mock(Histogram.class));

        given(mockNotificationClient.sendSms(anyString(), anyString(), anyMap(), isNull())).willReturn(mockSendSmsResponse);
        given(mockSendSmsResponse.getNotificationId()).willReturn(NOTIFICATION_ID);

        notificationService = new NotificationService(mockNotifyClientProvider, mockNotifyConfiguration, mockNotifyDirectDebitConfiguration,
                mockMetricRegistry);
    }

    @Test
    public void sendSecondFactorPasscodeSmsWithSignInTemplate() throws NotificationClientException {
        notificationService.sendSecondFactorPasscodeSms(PHONE_NUMBER, OTP, OtpNotifySmsTemplateId.SIGN_IN);

        verify(mockNotificationClient).sendSms(SIGN_IN_OTP_SMS_TEMPLATE_ID, PHONE_NUMBER_E164, Map.of("code", OTP), null);
    }

    @Test
    public void sendSecondFactorPasscodeSmsWithChangeSignIn2faToSmsTemplate() throws NotificationClientException {
        notificationService.sendSecondFactorPasscodeSms(PHONE_NUMBER, OTP, OtpNotifySmsTemplateId.CHANGE_SIGN_IN_2FA_TO_SMS);

        verify(mockNotificationClient).sendSms(CHANGE_SIGN_IN_2FA_TO_SMS_OTP_SMS_TEMPLATE_ID, PHONE_NUMBER_E164, Map.of("code", OTP), null);
    }

    @Test
    public void sendSecondFactorPasscodeSmsWithSelfInitiatedCreateNewUserAndServiceTemplate() throws NotificationClientException {
        notificationService.sendSecondFactorPasscodeSms(PHONE_NUMBER, OTP, OtpNotifySmsTemplateId.SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE);

        verify(mockNotificationClient).sendSms(SELF_INITIATED_CREATE_USER_AND_SERVICE_OTP_SMS_TEMPLATE_ID, PHONE_NUMBER_E164, Map.of("code", OTP),
                null);    
    }

    @Test
    public void sendSecondFactorPasscodeSmsWithCreateUserInResponseToInvitationToServiceTemplate() throws NotificationClientException {
        notificationService.sendSecondFactorPasscodeSms(PHONE_NUMBER, OTP, OtpNotifySmsTemplateId.CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE);

        verify(mockNotificationClient).sendSms(CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE_OTP_SMS_TEMPLATE_ID, PHONE_NUMBER_E164, Map.of("code", OTP),
                null);    
    }

}
