package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;

public class ServiceDbFixture {

    private final DatabaseTestHelper databaseHelper;
    private String[] gatewayAccountIds = new String[]{valueOf(nextInt())};
    private Integer id;
    private String externalId;
    private String name = Service.DEFAULT_NAME_VALUE;

    private ServiceDbFixture(DatabaseTestHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public static ServiceDbFixture serviceDbFixture(DatabaseTestHelper databaseTestHelper) {
        return new ServiceDbFixture(databaseTestHelper);
    }

    public ServiceDbFixture withGatewayAccountIds(String... gatewayAccountIds) {
        this.gatewayAccountIds = gatewayAccountIds;
        return this;
    }

    public Service insertService() {
        int serviceId = id == null ? nextInt() : id;
        String extId = externalId == null ? newId() : externalId;

        Service service = Service.from(serviceId, extId, name);
        databaseHelper.addService(service, gatewayAccountIds);

        return service;
    }

    public ServiceDbFixture withId(int id) {
        this.id = id;
        return this;
    }

    public ServiceDbFixture withExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }
}
