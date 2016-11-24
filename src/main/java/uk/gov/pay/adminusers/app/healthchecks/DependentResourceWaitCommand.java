package uk.gov.pay.adminusers.app.healthchecks;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;

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
        ApplicationStartupDependentResourceChecker applicationStartupDependentResourceChecker = new ApplicationStartupDependentResourceChecker(new ApplicationStartupDependentResource(conf));
        applicationStartupDependentResourceChecker.checkAndWaitForResources();
    }
}
