package uk.gov.pay.adminusers.pact;

import com.google.common.collect.ImmutableSetMultimap;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import uk.gov.pay.adminusers.pact.queuemessage.DisputeCreatedEventQueueContractTest;
import uk.gov.pay.adminusers.pact.queuemessage.DisputeEvidenceSubmittedEventQueueContractTest;
import uk.gov.pay.adminusers.pact.queuemessage.DisputeLostEventQueueContractTest;
import uk.gov.pay.adminusers.pact.queuemessage.DisputeWonEventQueueContractTest;
import uk.gov.service.payments.commons.testing.pact.provider.CreateTestSuite;


@RunWith(AllTests.class)
public class ConsumerContractTestSuite {

    public static TestSuite suite() {
        ImmutableSetMultimap<String, JUnit4TestAdapter> map = ImmutableSetMultimap.of(
                "connector", new JUnit4TestAdapter(DisputeCreatedEventQueueContractTest.class),
                "connector", new JUnit4TestAdapter(DisputeEvidenceSubmittedEventQueueContractTest.class),
                "connector", new JUnit4TestAdapter(DisputeWonEventQueueContractTest.class),
                "connector", new JUnit4TestAdapter(DisputeLostEventQueueContractTest.class));
        return CreateTestSuite.create(map);
    }
}
