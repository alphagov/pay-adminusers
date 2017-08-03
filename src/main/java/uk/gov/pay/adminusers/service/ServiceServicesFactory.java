package uk.gov.pay.adminusers.service;

public interface ServiceServicesFactory {

    ServiceCreator serviceCreator();

    ServiceUpdater serviceUpdater();

    ServiceUserRemover serviceUserRemover();

    ServiceCustomisationsUpdater serviceCustomisationsUpdater();
}
