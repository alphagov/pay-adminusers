package uk.gov.pay.adminusers.pact;

import com.google.common.collect.ImmutableSetMultimap;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import uk.gov.pay.commons.testing.pact.provider.CreateTestSuite;

import java.util.Map;

@RunWith(AllTests.class)
public class ContractTestSuite {

    public static TestSuite suite() {
        ImmutableSetMultimap<String, JUnit4TestAdapter> map = ImmutableSetMultimap.of(
                "products-ui", new JUnit4TestAdapter(ProductsUIContractTest.class),
                "selfservice", new JUnit4TestAdapter(SelfServiceContractTest.class));
        return CreateTestSuite.create(map);
    }
}
