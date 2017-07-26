package uk.gov.pay.adminusers.service;

/**
 * Factory for providing user services components
 *
 * instantiation facilitated by Guice assisted injects supported via FactoryModule
 * @see uk.gov.pay.adminusers.app.config.AdminUsersModule
 */
public interface UserServicesFactory {

    ServiceRoleUpdater serviceRoleUpdater();

    ServiceRoleCreator serviceRoleCreator();

    UserCreator userCreator();
}
