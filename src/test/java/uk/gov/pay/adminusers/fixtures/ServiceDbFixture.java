package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.RandomUtils.nextInt;


public class ServiceDbFixture {
    private final DatabaseTestHelper databaseTestHelper;
    private String gatewayAccountId = valueOf(nextInt());

    public ServiceDbFixture(DatabaseTestHelper databaseTestHelper) {

        this.databaseTestHelper = databaseTestHelper;
    }

    public static ServiceDbFixture aService(DatabaseTestHelper databaseTestHelper) {
       return new ServiceDbFixture(databaseTestHelper);
    }

    public ServiceDbFixture withGatewayAccountId(String gatewayAccountId) {
        this.gatewayAccountId = gatewayAccountId;
        return this;
    }

    public int build() {

        int serviceId = nextInt();
        databaseTestHelper.addService(serviceId, gatewayAccountId);
        return serviceId;
    }
}
