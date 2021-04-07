package uk.gov.pay.adminusers.service;

public interface UserServicesFactory {

    ServiceRoleUpdater serviceRoleUpdater();

    ServiceRoleCreator serviceRoleCreator();

    UserCreator userCreator();
}
