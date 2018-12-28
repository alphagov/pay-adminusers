package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.model.GoLiveStage;
import uk.gov.pay.adminusers.model.MerchantDetails;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class ServiceDbFixture {

    private final DatabaseTestHelper databaseHelper;
    private String[] gatewayAccountIds = new String[]{valueOf(nextInt())};
    private Integer id;
    private String externalId;
    private final String name = Service.DEFAULT_NAME_VALUE;
    private MerchantDetails merchantDetails = new MerchantDetails(
            "name", null, "line1", null, "city",
            "postcode", "country", null
    );
    private boolean collectBillingAddress = true;

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

    public ServiceDbFixture withMerchantDetails(MerchantDetails merchantDetails) {
        this.merchantDetails = merchantDetails;
        return this;
    }

    public ServiceDbFixture withCollectBillingAddress(boolean collectBillingAddress) {
        this.collectBillingAddress = collectBillingAddress;
        return this;
    }

    public Service insertService() {
        int serviceId = id == null ? nextInt() : id;
        String extId = externalId == null ? randomUuid() : externalId;

        Service service = Service.from(serviceId, extId, name);
        service.setMerchantDetails(merchantDetails);
        service.setCollectBillingAddress(collectBillingAddress);
        service.getServiceNames().put("en", service.getName());
        service.setGoLiveStage(GoLiveStage.NOT_STARTED);
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
