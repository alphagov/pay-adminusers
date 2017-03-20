package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.RandomUtils.nextInt;

public class ServiceDbFixture {
    private final DatabaseTestHelper databaseTestHelper;
    private String[] gatewayAccountIds = new String[]{valueOf(nextInt())};

    public ServiceDbFixture(DatabaseTestHelper databaseTestHelper) {
        this.databaseTestHelper = databaseTestHelper;
    }

    public static ServiceDbFixture serviceDbFixture(DatabaseTestHelper databaseTestHelper) {
        return new ServiceDbFixture(databaseTestHelper);
    }

    public ServiceDbFixture withGatewayAccountIds(String... gatewayAccountIds) {
        this.gatewayAccountIds = gatewayAccountIds;
        return this;
    }

    public int insertService() {
        int serviceId = nextInt();
        databaseTestHelper.addService(serviceId, gatewayAccountIds);
        return serviceId;
    }
}
