package uk.gov.pay.adminusers.pact;

import com.google.common.collect.ImmutableSetMultimap;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import uk.gov.pay.adminusers.pact.queuemessage.DisputeCreatedEventQueueContractTest;
import uk.gov.service.payments.commons.testing.pact.provider.CreateTestSuite;


@RunWith(AllTests.class)
public class ConsumerContractTestSuite {

    public static TestSuite suite() {
        ImmutableSetMultimap<String, JUnit4TestAdapter> map = ImmutableSetMultimap.of(
                "connector", new JUnit4TestAdapter(DisputeCreatedEventQueueContractTest.class));
        return CreateTestSuite.create(map);
    }
}
