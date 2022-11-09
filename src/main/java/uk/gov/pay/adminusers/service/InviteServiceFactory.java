package uk.gov.pay.adminusers.service;


public interface InviteServiceFactory {

    ServiceInviteCreator serviceInvite();

    UserInviteCreator userInvite();

    InviteFinder inviteFinder();

    InviteRouter inviteOtpRouter();

    ServiceOtpDispatcher dispatchServiceOtp();

    UserOtpDispatcher dispatchUserOtp();
}
