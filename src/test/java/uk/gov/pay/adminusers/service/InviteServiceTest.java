package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.exception.UserNotificationException;
import uk.gov.pay.adminusers.model.InviteOtpRequest;
import uk.gov.pay.adminusers.model.InviteValidateOtpRequest;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.ws.rs.WebApplicationException;
import java.util.Optional;

import static java.lang.String.valueOf;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_CODE;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_PASSWORD;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_TELEPHONE_NUMBER;
import static uk.gov.pay.adminusers.model.InviteType.NEW_USER_AND_NEW_SERVICE_SELF_SIGNUP;
import static uk.gov.pay.adminusers.model.InviteType.NEW_USER_INVITED_TO_EXISTING_SERVICE;
import static uk.gov.pay.adminusers.model.InviteType.SERVICE;
import static uk.gov.pay.adminusers.model.InviteType.USER;
import static uk.gov.pay.adminusers.model.Role.role;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    private static final String TELEPHONE_NUMBER = "+441134960000";
    private static final String PLAIN_PASSWORD = "my-secure-pass";

    @Mock
    private UserDao mockUserDao;
    @Mock
    private InviteDao mockInviteDao;
    @Mock
    private NotificationService mockNotificationService;
    @Mock
    private SecondFactorAuthenticator mockSecondFactorAuthenticator;

    private InviteService inviteService;
    private final ArgumentCaptor<InviteEntity> expectedInvite = ArgumentCaptor.forClass(InviteEntity.class);
    private final ArgumentCaptor<UserEntity> expectedInvitedUser = ArgumentCaptor.forClass(UserEntity.class);
    private final int passCode = 123456;
    private final String otpKey = "otpKey";
    private final String inviteCode = "code";
    private final String senderEmail = "sender@example.com";
    private final String email = "invited@example.com";
    private final int serviceId = 1;
    private final String senderExternalId = "12345";

    @BeforeEach
    void setUp() {
        inviteService = new InviteService(
                mockUserDao,
                mockInviteDao,
                mockNotificationService,
                mockSecondFactorAuthenticator,
                new LinksBuilder("http://localhost"),
                3
        );
    }

    private InviteEntity mocksCreateInvite() {
        ServiceEntity service = new ServiceEntity();
        service.setId(serviceId);

        UserEntity senderUser = new UserEntity();
        senderUser.setExternalId(senderExternalId);
        senderUser.setEmail(senderEmail);
        RoleEntity role = new RoleEntity(role(ADMIN.getId(), "admin", "Admin Role"));
        senderUser.addServiceRole(new ServiceRoleEntity(service, role));

        InviteEntity anInvite = anInvite(email, inviteCode, otpKey, role);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        
        return anInvite;
    }

    @Test
    void validateOtpAndCreateUser_shouldCreateInvitedUserOnSuccessfulInvite() {
        mocksCreateInvite();
        when(mockSecondFactorAuthenticator.authorize(otpKey, passCode)).thenReturn(true);

        InviteValidateOtpRequest inviteValidateOtpRequest = inviteValidateOtpRequest(inviteCode, passCode);
        inviteService.validateOtpAndCreateUser(inviteValidateOtpRequest);

        verify(mockUserDao).persist(expectedInvitedUser.capture());
        UserEntity createdUser = expectedInvitedUser.getValue();
        assertThat(createdUser.getEmail(), is(email));
        assertThat(createdUser.isDisabled(), is(Boolean.FALSE));
    }

    @Test
    void validateOtpAndCreateUser_shouldDisableInviteOnSuccessfulInvite() {
        mocksCreateInvite();
        when(mockSecondFactorAuthenticator.authorize(otpKey, passCode)).thenReturn(true);

        InviteValidateOtpRequest inviteValidateOtpRequest = inviteValidateOtpRequest(inviteCode, passCode);
        inviteService.validateOtpAndCreateUser(inviteValidateOtpRequest);

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity savedInvite = expectedInvite.getValue();
        assertThat(savedInvite.getCode(), is(inviteCode));
        assertThat(savedInvite.isDisabled(), is(Boolean.TRUE));
    }

    @Test
    void validateOtpAndCreateUser_shouldErrorWhenDisabled_evenIfOtpValidationIsSuccessful() {
        InviteEntity anInvite = mocksCreateInvite();
        anInvite.setDisabled(Boolean.TRUE);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));

        InviteValidateOtpRequest inviteValidateOtpRequest = inviteValidateOtpRequest(inviteCode, passCode);
        ValidateOtpAndCreateUserResult validateOtpAndCreateUserResult =
                inviteService.validateOtpAndCreateUser(inviteValidateOtpRequest);
        assertThat(validateOtpAndCreateUserResult.isError(), is(true));
        assertThat(validateOtpAndCreateUserResult.getError().getResponse().getStatus(), is(GONE.getStatusCode()));
    }

    @Test
    void validateOtpAndCreateUser_shouldErrorAndDisableWhenInvalidOtpValidation_ifMaxRetryExceeded() {
        InviteEntity anInvite = mocksCreateInvite();
        anInvite.setLoginCounter(2);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        int invalidPassCode = 1337;
        when(mockSecondFactorAuthenticator.authorize(otpKey, invalidPassCode)).thenReturn(false);

        InviteValidateOtpRequest inviteValidateOtpRequest = inviteValidateOtpRequest(inviteCode, invalidPassCode);
        ValidateOtpAndCreateUserResult validateOtpAndCreateUserResult =
                inviteService.validateOtpAndCreateUser(inviteValidateOtpRequest);
        assertThat(validateOtpAndCreateUserResult.isError(), is(true));
        assertThat(validateOtpAndCreateUserResult.getError().getResponse().getStatus(), is(GONE.getStatusCode()));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity savedInvite = expectedInvite.getValue();
        assertThat(savedInvite.getLoginCounter(), is(3));
        assertThat(savedInvite.isDisabled(), is(Boolean.TRUE));
    }

    @Test
    void validateOtpAndCreateUser_shouldErrorAndIncrementLoginCounterWhenInvalidOtpValidation() {
        InviteEntity anInvite = mocksCreateInvite();
        anInvite.setLoginCounter(1);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        int invalidPassCode = 1337;
        when(mockSecondFactorAuthenticator.authorize(otpKey, invalidPassCode)).thenReturn(false);

        InviteValidateOtpRequest inviteValidateOtpRequest = inviteValidateOtpRequest(inviteCode, invalidPassCode);
        ValidateOtpAndCreateUserResult validateOtpAndCreateUserResult =
                inviteService.validateOtpAndCreateUser(inviteValidateOtpRequest);
        assertThat(validateOtpAndCreateUserResult.isError(), is(true));
        assertThat(validateOtpAndCreateUserResult.getError().getResponse().getStatus(), is(UNAUTHORIZED.getStatusCode()));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity savedInvite = expectedInvite.getValue();
        assertThat(savedInvite.getLoginCounter(), is(2));
        assertThat(savedInvite.isDisabled(), is(Boolean.FALSE));
    }

    @Test
    void validateOtpAndCreateUser_shouldCreateInvitedUserAndResetLoginCounterAndDisableInviteWhenValidOtpValidation() {
        InviteEntity anInvite = mocksCreateInvite();
        anInvite.setLoginCounter(2);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(anInvite));
        when(mockSecondFactorAuthenticator.authorize(otpKey, passCode)).thenReturn(true);

        InviteValidateOtpRequest inviteValidateOtpRequest = inviteValidateOtpRequest(inviteCode, passCode);
        inviteService.validateOtpAndCreateUser(inviteValidateOtpRequest);

        verify(mockUserDao).persist(expectedInvitedUser.capture());
        UserEntity createdUser = expectedInvitedUser.getValue();
        assertThat(createdUser.getEmail(), is(email));
        assertThat(createdUser.isDisabled(), is(Boolean.FALSE));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity savedInvite = expectedInvite.getValue();
        assertThat(savedInvite.getLoginCounter(), is(0));
        assertThat(savedInvite.isDisabled(), is(Boolean.TRUE));
    }

    @Test
    void validateOtpAndCreateUser_shouldErrorWhenInviteNotFound() {
        String notFoundInviteCode = "not-found-invite-code";
        when(mockInviteDao.findByCode(notFoundInviteCode)).thenReturn(Optional.empty());
        InviteValidateOtpRequest inviteValidateOtpRequest = inviteValidateOtpRequest(notFoundInviteCode, passCode);
        ValidateOtpAndCreateUserResult validateOtpAndCreateUserResult =
                inviteService.validateOtpAndCreateUser(inviteValidateOtpRequest);
        assertThat(validateOtpAndCreateUserResult.isError(), is(true));
        assertThat(validateOtpAndCreateUserResult.getError().getResponse().getStatus(), is(NOT_FOUND.getStatusCode()));
    }

    @Test
    void generateOtp_shouldSendNotificationOnSuccessfulServiceInviteUpdate() throws Exception {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        inviteEntity.setType(SERVICE);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(TELEPHONE_NUMBER), eq(valueOf(passCode)), eq(SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE)))
                .thenReturn("random-notify-id");

        inviteService.reGenerateOtp(inviteOtpRequestFrom(inviteCode, TELEPHONE_NUMBER, PLAIN_PASSWORD));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(TELEPHONE_NUMBER));
    }

    @Test
    void generateOtp_shouldStillUpdateTheServiceInviteWhen2FAFails() throws Exception {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        inviteEntity.setType(SERVICE);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(TELEPHONE_NUMBER), eq(valueOf(passCode)), eq(SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE)))
                .thenThrow(new UserNotificationException("Error sending SMS", new Exception()));

        inviteService.reGenerateOtp(inviteOtpRequestFrom(inviteCode, TELEPHONE_NUMBER, PLAIN_PASSWORD));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(TELEPHONE_NUMBER));
    }

    @Test
    void generateOtp_shouldSendNotificationOnSuccessfulUserInviteUpdate() throws Exception {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        inviteEntity.setType(USER);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(TELEPHONE_NUMBER), eq(valueOf(passCode)),
                eq(CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE))).thenReturn("random-notify-id");

        inviteService.reGenerateOtp(inviteOtpRequestFrom(inviteCode, TELEPHONE_NUMBER, PLAIN_PASSWORD));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(TELEPHONE_NUMBER));
    }

    @Test
    void generateOtp_shouldStillUpdateTheUserInviteWhen2FAFails() throws Exception {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        inviteEntity.setType(USER);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(TELEPHONE_NUMBER), eq(valueOf(passCode)),
                eq(CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE))).thenThrow(new UserNotificationException("Error sending SMS", new Exception()));

        inviteService.reGenerateOtp(inviteOtpRequestFrom(inviteCode, TELEPHONE_NUMBER, PLAIN_PASSWORD));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(TELEPHONE_NUMBER));
    }

    @Test
    void generateOtp_shouldSendNotificationOnSuccessfulServiceInviteUpdate__newEnumValue() throws Exception {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        inviteEntity.setType(NEW_USER_AND_NEW_SERVICE_SELF_SIGNUP);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(TELEPHONE_NUMBER), eq(valueOf(passCode)), eq(SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE)))
                .thenReturn("random-notify-id");

        inviteService.reGenerateOtp(inviteOtpRequestFrom(inviteCode, TELEPHONE_NUMBER, PLAIN_PASSWORD));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(TELEPHONE_NUMBER));
    }

    @Test
    void generateOtp_shouldStillUpdateTheServiceInviteWhen2FAFails__newEnumValue() throws Exception {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        inviteEntity.setType(NEW_USER_AND_NEW_SERVICE_SELF_SIGNUP);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(TELEPHONE_NUMBER), eq(valueOf(passCode)), eq(SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE)))
                .thenThrow(new UserNotificationException("Error sending SMS", new Exception()));

        inviteService.reGenerateOtp(inviteOtpRequestFrom(inviteCode, TELEPHONE_NUMBER, PLAIN_PASSWORD));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(TELEPHONE_NUMBER));
    }

    @Test
    void generateOtp_shouldSendNotificationOnSuccessfulUserInviteUpdate__newEnumValue() throws Exception {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        inviteEntity.setType(NEW_USER_INVITED_TO_EXISTING_SERVICE);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(TELEPHONE_NUMBER), eq(valueOf(passCode)),
                eq(CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE))).thenReturn("random-notify-id");

        inviteService.reGenerateOtp(inviteOtpRequestFrom(inviteCode, TELEPHONE_NUMBER, PLAIN_PASSWORD));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(TELEPHONE_NUMBER));
    }

    @Test
    void generateOtp_shouldStillUpdateTheUserInviteWhen2FAFails__newEnumValue() throws Exception {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        inviteEntity.setType(NEW_USER_INVITED_TO_EXISTING_SERVICE);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(TELEPHONE_NUMBER), eq(valueOf(passCode)),
                eq(CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE))).thenThrow(new UserNotificationException("Error sending SMS", new Exception()));

        inviteService.reGenerateOtp(inviteOtpRequestFrom(inviteCode, TELEPHONE_NUMBER, PLAIN_PASSWORD));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(TELEPHONE_NUMBER));
    }

    @Test
    void validateOtp_shouldReturnTrueOnValidInviteAndValidOtp() {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);

        when(mockSecondFactorAuthenticator.authorize(otpKey, passCode)).thenReturn(true);

        Optional<WebApplicationException> validationResult = inviteService.validateOtp(inviteEntity, passCode);

        assertThat(validationResult.isPresent(), is(false));
    }

    @Test
    void validateOtp_shouldReturnFalseOnValidInviteAndInValidOtp() {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        
        Optional<WebApplicationException> validationResult = inviteService.validateOtp(inviteEntity, passCode);

        assertThat(validationResult.isPresent(), is(true));
        assertThat(validationResult.get().getResponse().getStatus(), is(401));
    }

    @Test
    void validateOtp_shouldReturnFalseOnValidInviteAndValidOtpAndEntityDisabled() {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        inviteEntity.setDisabled(true);
 
        Optional<WebApplicationException> validationResult = inviteService.validateOtp(inviteEntity, passCode);

        assertThat(validationResult.isPresent(), is(true));
        assertThat(validationResult.get().getResponse().getStatus(), is(410));
    }

    private InviteOtpRequest inviteOtpRequestFrom(String code, String telephoneNumber, String password) {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put(FIELD_CODE, code);
        json.put(FIELD_TELEPHONE_NUMBER, telephoneNumber);
        json.put(FIELD_PASSWORD, password);
        return InviteOtpRequest.from(json);
    }

    private InviteValidateOtpRequest inviteValidateOtpRequest(String inviteCode, int otpCode) {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put(InviteValidateOtpRequest.FIELD_CODE, inviteCode);
        json.put(InviteValidateOtpRequest.FIELD_OTP, otpCode);
        return InviteValidateOtpRequest.from(json);
    }

    private InviteEntity anInvite(String email, String code, String otpKey, RoleEntity roleEntity) {
        return new InviteEntity(email, code, otpKey, roleEntity);
    }

}
