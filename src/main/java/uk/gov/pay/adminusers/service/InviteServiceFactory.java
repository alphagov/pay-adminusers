package uk.gov.pay.adminusers.service;


public interface InviteServiceFactory {

    SelfRegistrationInviteCreator selfRegistrationInviteCreator();

    JoinServiceInviteCreator joinServiceInviteCreator();

    InviteFinder inviteFinder();
}
