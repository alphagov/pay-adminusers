package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.model.InviteOtpRequest;
import uk.gov.pay.adminusers.model.InviteValidateOtpRequest;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.ws.rs.WebApplicationException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.lang.String.valueOf;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_CODE;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_PASSWORD;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_TELEPHONE_NUMBER;
import static uk.gov.pay.adminusers.model.Role.role;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

@RunWith(MockitoJUnitRunner.class)
public class InviteServiceTest {

    private static final String SELFSERVICE_URL = "http://selfservice";

    @Mock
    private RoleDao mockRoleDao;
    @Mock
    private ServiceDao mockServiceDao;
    @Mock
    private UserDao mockUserDao;
    @Mock
    private InviteDao mockInviteDao;
    @Mock
    private AdminUsersConfig mockConfig;
    @Mock
    private NotificationService mockNotificationService;
    @Mock
    private SecondFactorAuthenticator mockSecondFactorAuthenticator;

    private InviteService inviteService;
    private ArgumentCaptor<InviteEntity> expectedInvite = ArgumentCaptor.forClass(InviteEntity.class);
    private ArgumentCaptor<UserEntity> expectedInvitedUser = ArgumentCaptor.forClass(UserEntity.class);
    private int passCode = 123456;
    private String otpKey = "otpKey";
    private String inviteCode = "code";
    private String senderEmail = "sender@example.com";
    private String email = "invited@example.com";
    private int serviceId = 1;
    private String senderExternalId = "12345";
    private String roleName = "view-only";

    @Before
    public void setup() {
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
    public void validateOtpAndCreateUser_shouldCreateInvitedUserOnSuccessfulInvite() {

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
    public void validateOtpAndCreateUser_shouldDisableInviteOnSuccessfulInvite() {

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
    public void validateOtpAndCreateUser_shouldErrorWhenDisabled_evenIfOtpValidationIsSuccessful() {

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
    public void validateOtpAndCreateUser_shouldErrorAndDisableWhenInvalidOtpValidation_ifMaxRetryExceeded() {

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
    public void validateOtpAndCreateUser_shouldErrorAndIncrementLoginCounterWhenInvalidOtpValidation() {

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
    public void validateOtpAndCreateUser_shouldCreateInvitedUserAndResetLoginCounterAndDisableInviteWhenValidOtpValidation() {

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
    public void validateOtpAndCreateUser_shouldErrorWhenInviteNotFound() {

        String notFoundInviteCode = "not-found-invite-code";
        when(mockInviteDao.findByCode(notFoundInviteCode)).thenReturn(Optional.empty());
        InviteValidateOtpRequest inviteValidateOtpRequest = inviteValidateOtpRequest(notFoundInviteCode, passCode);
        ValidateOtpAndCreateUserResult validateOtpAndCreateUserResult =
                inviteService.validateOtpAndCreateUser(inviteValidateOtpRequest);
        assertThat(validateOtpAndCreateUserResult.isError(), is(true));
        assertThat(validateOtpAndCreateUserResult.getError().getResponse().getStatus(), is(NOT_FOUND.getStatusCode()));
    }

    @Test
    public void generateOtp_shouldSendNotificationOnSuccessfulInviteUpdate() throws ExecutionException, InterruptedException {

        String telephoneNumber = "+441134960000";
        String plainPassword = "my-secure-pass";

        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        CompletableFuture<String> errorPromise = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("some error from notify");
        });
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(telephoneNumber), eq(valueOf(passCode))))
                .thenReturn(errorPromise);

        inviteService.reGenerateOtp(inviteOtpRequestFrom(inviteCode, telephoneNumber, plainPassword)).get();

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(telephoneNumber));
        assertThat(errorPromise.isCompletedExceptionally(), is(true));
    }

    @Test
    public void generateOtp_shouldStillUpdateTheInviteWhen2FAFails() throws ExecutionException, InterruptedException {

        String telephoneNumber = "+441134960000";
        String plainPassword = "my-secure-pass";

        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        CompletableFuture<String> notifyPromise = CompletableFuture.completedFuture("random-notify-id");
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(telephoneNumber), eq(valueOf(passCode))))
                .thenReturn(notifyPromise);

        inviteService.reGenerateOtp(inviteOtpRequestFrom(inviteCode, telephoneNumber, plainPassword)).get();

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(telephoneNumber));
        assertThat(notifyPromise.isDone(), is(true));
    }

    @Test
    public void validateOtp_shouldReturnTrueOnValidInviteAndValidOtp() {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);

        when(mockSecondFactorAuthenticator.authorize(otpKey, passCode)).thenReturn(true);

        Optional<WebApplicationException> validationResult = inviteService.validateOtp(inviteEntity, passCode);

        assertThat(validationResult.isPresent(), is(false));
    }

    @Test
    public void validateOtp_shouldReturnFalseOnValidInviteAndInValidOtp() {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        
        Optional<WebApplicationException> validationResult = inviteService.validateOtp(inviteEntity, passCode);

        assertThat(validationResult.isPresent(), is(true));
        assertThat(validationResult.get().getResponse().getStatus(), is(401));
    }

    @Test
    public void validateOtp_shouldReturnFalseOnValidInviteAndValidOtpAndEntityDisabled() {
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
