package uk.gov.pay.adminusers.app.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PingTest {

    @Test
    public void testPing() {
        HealthCheck.Result pingExecution = new Ping().execute();
        assertThat(pingExecution.isHealthy(), is(true));
    }
}
