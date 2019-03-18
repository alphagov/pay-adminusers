package uk.gov.pay.adminusers.pact;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        SelfserviceContractTest.class,
        FrontendContractTest.class
})
public class ContractTestSuite {

}
