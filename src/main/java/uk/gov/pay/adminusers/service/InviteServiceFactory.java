package uk.gov.pay.adminusers.service;


public interface InviteServiceFactory {

    ServiceInviteCreator serviceInvite();

    UserInviteCreator userInvite();

    InviteFinder inviteFinder();

    InviteRouter inviteCompleteRouter();

    ServiceInviteCompleter completeServiceInvite();

    ExistingUserInviteCompleter completeExistingUserInvite();

    InviteRouter inviteOtpRouter();

    ServiceOtpDispatcher dispatchServiceOtp();

    UserOtpDispatcher dispatchUserOtp();
}
