package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.model.CompleteInviteResponse;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchRequest;

import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.valueOf;
import static java.time.ZonedDateTime.now;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.fixtures.InviteEntityFixture.anInviteEntity;
import static uk.gov.pay.adminusers.fixtures.ServiceEntityFixture.aServiceEntity;
import static uk.gov.pay.adminusers.fixtures.UserEntityFixture.aUserEntity;
import static uk.gov.pay.adminusers.model.SecondFactorMethod.APP;
import static uk.gov.pay.adminusers.model.SecondFactorMethod.SMS;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    private static final String TELEPHONE_NUMBER = "+441134960000";
    private static final String PLAIN_PASSWORD = "my-secure-pass";

    @Mock
    private InviteDao mockInviteDao;
    @Mock
    private UserDao mockUserDao;
    @Mock
    private NotificationService mockNotificationService;
    @Mock
    private SecondFactorAuthenticator mockSecondFactorAuthenticator;
    @Mock
    private PasswordHasher mockPasswordHasher;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Captor
    private ArgumentCaptor<UserEntity> userEntityArgumentCaptor;

    private InviteService inviteService;
    private final ArgumentCaptor<InviteEntity> expectedInvite = ArgumentCaptor.forClass(InviteEntity.class);
    private final int passCode = 123456;
    private final String otpKey = "otpKey";
    private final String inviteCode = "code";
    private final String email = "foo@example.com";
    private final String baseUrl = "http://localhost";

    @BeforeEach
    void setUp() {
        inviteService = new InviteService(
                mockInviteDao,
                mockUserDao,
                mockNotificationService,
                mockSecondFactorAuthenticator,
                mockPasswordHasher,
                new LinksBuilder(baseUrl),
                3
        );
    }
    
    @Nested
    class sendOtp {
        @Test
        void sendOtp_shouldSendNotificationWithCreateUserInResponseToInvitationToServiceTemplate_whenInviteHasService() {
            InviteEntity inviteEntity = new InviteEntity();
            inviteEntity.setTelephoneNumber(TELEPHONE_NUMBER);
            inviteEntity.setOtpKey(otpKey);
            inviteEntity.setService(aServiceEntity().build());

            when(mockInviteDao.findByCode(eq(inviteCode))).thenReturn(Optional.of(inviteEntity));
            when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
            when(mockNotificationService.sendSecondFactorPasscodeSms(eq(TELEPHONE_NUMBER), eq(valueOf(passCode)),
                    eq(CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE))).thenReturn("random-notify-id");

            inviteService.sendOtp(inviteCode);
        }
        
        @Test
        void sendOtp_shouldSendNotificationWithSelfInitiatedCreateNewUserAndServiceTemplate_whenInviteDoesNotHaveService() {
            InviteEntity inviteEntity = new InviteEntity();
            inviteEntity.setTelephoneNumber(TELEPHONE_NUMBER);
            inviteEntity.setOtpKey(otpKey);
            inviteEntity.setService(null);

            when(mockInviteDao.findByCode(eq(inviteCode))).thenReturn(Optional.of(inviteEntity));
            when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
            when(mockNotificationService.sendSecondFactorPasscodeSms(eq(TELEPHONE_NUMBER), eq(valueOf(passCode)),
                    eq(SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE))).thenReturn("random-notify-id");

            inviteService.sendOtp(inviteCode);
        }

        @Test
        void sendOtp_shouldThrowWhenInviteNotFound() {
            when(mockInviteDao.findByCode(eq(inviteCode))).thenReturn(Optional.empty());

            WebApplicationException exception = assertThrows(WebApplicationException.class, () -> inviteService.sendOtp(inviteCode));
            assertThat(exception.getResponse().getStatus(), is(NOT_FOUND.getStatusCode()));
        }

        @Test
        void sendOtp_shouldThrowWhenInviteDoesNotHaveTelephoneNumber() {
            InviteEntity inviteEntity = new InviteEntity();
            inviteEntity.setTelephoneNumber(null);
            inviteEntity.setOtpKey(otpKey);
            inviteEntity.setService(null);

            when(mockInviteDao.findByCode(eq(inviteCode))).thenReturn(Optional.of(inviteEntity));

            WebApplicationException exception = assertThrows(WebApplicationException.class, () -> inviteService.sendOtp(inviteCode));
            assertThat(exception.getResponse().getStatus(), is(PRECONDITION_FAILED.getStatusCode()));
        }
    }
    
    @Nested
    class reprovisionOtp {
        @Test
        void reprovisionOtp_shouldUpdateOtpKey_andReturnUpdatedInvite() {
            String newOtpKey = "this-is-a-new-otp-key";
            
            InviteEntity inviteEntity = anInviteEntity().withCode(inviteCode).withOtpKey(otpKey).build();

            when(mockInviteDao.findByCode(eq(inviteCode))).thenReturn(Optional.of(inviteEntity));
            when(mockSecondFactorAuthenticator.generateNewBase32EncodedSecret()).thenReturn(newOtpKey);

            Invite invite = inviteService.reprovisionOtp(inviteCode);

            assertThat(invite.getOtpKey(), is(newOtpKey));
            assertThat(inviteEntity.getOtpKey(), is(newOtpKey));

            verify(mockInviteDao).merge(inviteEntity);
        }
        
        @Test
        void reprovisionOtp_shouldThrowWhenInviteNotFound() {
            when(mockInviteDao.findByCode(eq(inviteCode))).thenReturn(Optional.empty());

            WebApplicationException exception = assertThrows(WebApplicationException.class, () -> inviteService.reprovisionOtp(inviteCode));
            assertThat(exception.getResponse().getStatus(), is(NOT_FOUND.getStatusCode()));
        }
    }

    @Nested
    class validateOtp {
        @Test
        void validateOtp_shouldNotThrowForValidInviteAndValidOtp() {
            InviteEntity inviteEntity = new InviteEntity();
            inviteEntity.setOtpKey(otpKey);

            when(mockSecondFactorAuthenticator.authorize(otpKey, passCode)).thenReturn(true);

            assertDoesNotThrow(() -> inviteService.validateOtp(inviteEntity, passCode));
        }

        @Test
        void validateOtp_shouldReturnFalseOnValidInviteAndInValidOtp() {
            InviteEntity inviteEntity = new InviteEntity();
            inviteEntity.setOtpKey(otpKey);

            WebApplicationException exception = assertThrows(WebApplicationException.class, () -> inviteService.validateOtp(inviteEntity, passCode));
            assertThat(exception.getResponse().getStatus(), is(401));
        }

        @Test
        void validateOtp_shouldReturnFalseOnValidInviteAndValidOtpAndEntityDisabled() {
            InviteEntity inviteEntity = new InviteEntity();
            inviteEntity.setOtpKey(otpKey);
            inviteEntity.setDisabled(true);

            WebApplicationException exception = assertThrows(WebApplicationException.class, () -> inviteService.validateOtp(inviteEntity, passCode));
            assertThat(exception.getResponse().getStatus(), is(410));
        }
    }

    @Nested
    class updateInvite {
        @Test
        void shouldUpdateInvite() {
            InviteEntity inviteEntity = anInviteEntity().withCode(inviteCode).build();
            when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));

            var updatePasswordRequest = JsonPatchRequest.from(objectMapper.valueToTree(
                    Map.of("path", "password",
                            "op", "replace",
                            "value", PLAIN_PASSWORD)
            ));
            var updatePhoneNumberRequest = JsonPatchRequest.from(objectMapper.valueToTree(
                    Map.of("path", "telephone_number",
                            "op", "replace",
                            "value", TELEPHONE_NUMBER)
            ));

            String hashedPassword = "hashed";
            when(mockPasswordHasher.hash(PLAIN_PASSWORD)).thenReturn(hashedPassword);

            inviteService.updateInvite(inviteCode, List.of(updatePasswordRequest, updatePhoneNumberRequest));
            assertThat(inviteEntity.getPassword(), is(hashedPassword));
            assertThat(inviteEntity.getTelephoneNumber(), is(TELEPHONE_NUMBER));
        }

        @Test
        void shouldThrowWhenInviteNotFound() {
            when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.empty());

            var updateRequest = JsonPatchRequest.from(objectMapper.valueToTree(
                    Map.of("path", "password",
                            "op", "replace",
                            "value", PLAIN_PASSWORD)
            ));

            var exception = assertThrows(WebApplicationException.class, () -> inviteService.updateInvite(inviteCode, List.of(updateRequest)));
            assertThat(exception.getResponse().getStatus(), is(NOT_FOUND.getStatusCode()));
        }

        @Test
        void shouldThrowWhenPathNotSupported() {
            InviteEntity inviteEntity = anInviteEntity().withCode(inviteCode).build();
            when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));

            var updateRequest = JsonPatchRequest.from(objectMapper.valueToTree(
                    Map.of("path", "foo",
                            "op", "replace",
                            "value", "bar")
            ));

            var exception = assertThrows(WebApplicationException.class, () -> inviteService.updateInvite(inviteCode, List.of(updateRequest)));
            assertThat(exception.getResponse().getStatus(), is(BAD_REQUEST.getStatusCode()));
        }
    }

    @Nested
    class Complete {

        RoleEntity adminRole = new RoleEntity(new Role(2, RoleName.ADMIN, "Administrator"));
        
        @Test
        void shouldThrowNotFoundExceptionWhenInviteNotFound() {
            when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.empty());

            WebApplicationException webApplicationException = assertThrows(WebApplicationException.class, () -> inviteService.complete(inviteCode, SMS));
            assertThat(webApplicationException.getResponse().getStatus(), is(404));
        }

        @Test
        void shouldThrowExceptionWhenInviteIsDisabled() {
            InviteEntity inviteEntity = anInviteEntity().withDisabled(true).build();
            when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));

            WebApplicationException webApplicationException = assertThrows(WebApplicationException.class, () -> inviteService.complete(inviteCode, SMS));
            assertThat(webApplicationException.getResponse().getStatus(), is(410));
        }

        @Test
        void shouldThrowExceptionWhenInviteIsExpired() {
            InviteEntity inviteEntity = anInviteEntity()
                    .withExpiryDate(now().minusSeconds(1))
                    .build();
            when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));

            WebApplicationException webApplicationException = assertThrows(WebApplicationException.class, () -> inviteService.complete(inviteCode, SMS));
            assertThat(webApplicationException.getResponse().getStatus(), is(410));
        }

        @Test
        @DisplayName("An invite inviting a user to a service when the user already exists completes successfully")
        void shouldAddServiceRoleToUserWhenExists() {
            ServiceEntity serviceEntity = aServiceEntity().build();
            UserEntity userEntity = aUserEntity().build();

            InviteEntity inviteEntity = anInviteEntity()
                    .withEmail(email)
                    .withService(serviceEntity)
                    .withRole(adminRole)
                    .build();

            when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
            when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(userEntity));

            CompleteInviteResponse response = inviteService.complete(inviteCode, null);

            verify(mockUserDao).merge(userEntityArgumentCaptor.capture());
            UserEntity updatedUser = userEntityArgumentCaptor.getValue();

            assertThat(response.getUserExternalId(), is(userEntity.getExternalId()));
            assertThat(response.getServiceExternalId(), is(serviceEntity.getExternalId()));

            assertThat(inviteEntity.isDisabled(), is(true));
            Optional<ServiceRoleEntity> userServiceRole = updatedUser.getServicesRole(serviceEntity.getExternalId());
            assertThat(userServiceRole.isPresent(), is(true));
            assertThat(userServiceRole.get().getRole().getId(), is(adminRole.getId()));
        }

        @Test
        @DisplayName("An invite inviting a user to a service when the user does not exist completes successfully")
        void shouldCreateUserAndAddToServiceWhenUserDoesNotExist() {
            ServiceEntity serviceEntity = aServiceEntity().build();
            InviteEntity inviteEntity = anInviteEntity()
                    .withEmail(email)
                    .withService(serviceEntity)
                    .withRole(adminRole)
                    .build();

            when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
            when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());

            CompleteInviteResponse response = inviteService.complete(inviteCode, APP);

            verify(mockUserDao).persist(userEntityArgumentCaptor.capture());
            UserEntity persistedUser = userEntityArgumentCaptor.getValue();

            assertThat(response.getUserExternalId(), is(persistedUser.getExternalId()));
            assertThat(response.getServiceExternalId(), is(serviceEntity.getExternalId()));

            assertThat(inviteEntity.isDisabled(), is(true));
            assertThat(persistedUser.getEmail(), is(inviteEntity.getEmail()));
            assertThat(persistedUser.getSecondFactor(), is(APP));
            Optional<ServiceRoleEntity> userServiceRole = persistedUser.getServicesRole(serviceEntity.getExternalId());
            assertThat(userServiceRole.isPresent(), is(true));
            assertThat(userServiceRole.get().getRole().getId(), is(adminRole.getId()));
        }

        @Test
        @DisplayName("A self-registration invite completes successfully")
        void shouldCreateUserWithoutServiceWhenNoServiceOnInvite() {
            InviteEntity inviteEntity = anInviteEntity()
                    .withEmail(email)
                    .build();

            when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
            when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());

            CompleteInviteResponse response = inviteService.complete(inviteCode, SMS);

            verify(mockUserDao).persist(userEntityArgumentCaptor.capture());
            UserEntity persistedUser = userEntityArgumentCaptor.getValue();

            assertThat(response.getUserExternalId(), is(persistedUser.getExternalId()));
            assertThat(response.getServiceExternalId(), is(nullValue()));

            assertThat(inviteEntity.isDisabled(), is(true));
            assertThat(persistedUser.getEmail(), is(inviteEntity.getEmail()));
            assertThat(persistedUser.getServicesRoles(), hasSize(0));
        }

        @Test
        @DisplayName("An invite for an existing user throws an exception when no service is set on the invite")
        void shouldThrowExceptionWhenUserExistsAndNoServiceOnInvite() {
            UserEntity userEntity = aUserEntity().build();

            InviteEntity inviteEntity = anInviteEntity()
                    .withEmail(email)
                    .build();

            when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
            when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(userEntity));

            WebApplicationException exception = assertThrows(WebApplicationException.class, () -> inviteService.complete(inviteCode, null));
            assertThat(exception.getResponse().getStatus(), is(409));
            Map<String, List<String>> entity = (Map<String, List<String>>) exception.getResponse().getEntity();
            assertThat(entity.get("errors").get(0), is("User with email [foo@example.com] already exists for self-registration invite"));
        }


        @Test
        @DisplayName("An invite for a non-existent user throws an exception when no second factor method is supplied")
        void shouldThrowExceptionWhenUserDoesNotExistAndNo2FAMethodSupplied() {
            InviteEntity inviteEntity = anInviteEntity()
                    .withEmail(email)
                    .build();

            when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
            when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());

            WebApplicationException exception = assertThrows(WebApplicationException.class, () -> inviteService.complete(inviteCode, null));
            assertThat(exception.getResponse().getStatus(), is(400));
            Map<String, List<String>> entity = (Map<String, List<String>>) exception.getResponse().getEntity();
            assertThat(entity.get("errors").get(0), is("Second factor not provided when attempting to complete an invite for a non-existent user. invite-code = a-code"));
        }

        @Test
        @DisplayName("An invite inviting a non-existent user to a service throws an exception when the invite doesn't have a role set")
        void shouldThrowExceptionWhenInviteHasServiceButNoRoleForNonExistentUser() {
            ServiceEntity serviceEntity = aServiceEntity().build();
            InviteEntity inviteEntity = anInviteEntity()
                    .withEmail(email)
                    .withService(serviceEntity)
                    .build();

            when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
            when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());

            WebApplicationException exception = assertThrows(WebApplicationException.class, () -> inviteService.complete(inviteCode, SMS));
            assertThat(exception.getResponse().getStatus(), is(500));
            Map<String, List<String>> entity = (Map<String, List<String>>) exception.getResponse().getEntity();
            assertThat(entity.get("errors").get(0), is("Invite with code a-code to invite user to a service does not have a role set"));
        }

        @Test
        @DisplayName("An invite inviting an existing user to a service throws an exception when the invite doesn't have a role set")
        void shouldThrowExceptionWhenInviteHasServiceButNoRoleForNonExistingUser() {
            ServiceEntity serviceEntity = aServiceEntity().build();
            UserEntity userEntity = aUserEntity().build();

            InviteEntity inviteEntity = anInviteEntity()
                    .withEmail(email)
                    .withService(serviceEntity)
                    .build();

            when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
            when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(userEntity));

            when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
            when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());

            WebApplicationException exception = assertThrows(WebApplicationException.class, () -> inviteService.complete(inviteCode, SMS));
            assertThat(exception.getResponse().getStatus(), is(500));
            Map<String, List<String>> entity = (Map<String, List<String>>) exception.getResponse().getEntity();
            assertThat(entity.get("errors").get(0), is("Invite with code a-code to invite user to a service does not have a role set"));
        }
    }

}
