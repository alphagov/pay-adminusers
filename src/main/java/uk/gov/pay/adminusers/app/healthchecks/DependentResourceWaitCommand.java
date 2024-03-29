package uk.gov.pay.adminusers.app.healthchecks;

import io.dropwizard.core.cli.ConfiguredCommand;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.service.payments.commons.utils.startup.ApplicationStartupDependentResourceChecker;
import uk.gov.service.payments.commons.utils.startup.DatabaseStartupResource;

public class DependentResourceWaitCommand extends ConfiguredCommand<AdminUsersConfig> {
    public DependentResourceWaitCommand() {
        super("waitOnDependencies", "Waits for dependent resources to become available");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
    }

    @Override
    protected void run(Bootstrap<AdminUsersConfig> bs, Namespace ns, AdminUsersConfig conf) {
        new ApplicationStartupDependentResourceChecker(new DatabaseStartupResource(conf.getDataSourceFactory()))
                .checkAndWaitForResource();
    }
}
