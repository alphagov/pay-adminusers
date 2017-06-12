package uk.gov.pay.adminusers.service;


public interface InviteServiceFactory {

    ServiceInviteCreator serviceInvite();

    UserInviteCreator userInvite();
}
