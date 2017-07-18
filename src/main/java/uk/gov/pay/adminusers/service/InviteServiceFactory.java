package uk.gov.pay.adminusers.service;


public interface InviteServiceFactory {

    ServiceInviteCreator serviceInvite();

    UserInviteCreator userInvite();

    InviteFinder inviteFinder();

    InviteRouter inviteCompleteRouter();

    ServiceInviteCompleter completeServiceInvite();

    UserInviteCompleter completeUserInvite();

    InviteRouter inviteOtpRouter();

    ServiceOtpDispatcher dispatchServiceOtp();

    UserOtpDispatcher dispatchUserOtp();
}
