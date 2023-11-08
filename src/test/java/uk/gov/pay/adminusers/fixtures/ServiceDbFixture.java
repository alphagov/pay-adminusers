package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.model.GoLiveStage;
import uk.gov.pay.adminusers.model.MerchantDetails;
import uk.gov.pay.adminusers.model.PspTestAccountStage;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceName;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.lang.String.valueOf;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class ServiceDbFixture {

    private final DatabaseTestHelper databaseHelper;
    private List<String> gatewayAccountIds = List.of(valueOf(nextInt()));
    private Integer id;
    private String externalId;
    private String name = Service.DEFAULT_NAME_VALUE;
    private MerchantDetails merchantDetails = new MerchantDetails(
            "name", null, "line1", null, "city",
            "postcode", "country", null, null
    );
    private boolean collectBillingAddress = true;
    private boolean experimentalFeaturesEnabled = false;
    
    private boolean takesPaymentsOverPhone = false;
    private GoLiveStage goLiveStage = GoLiveStage.NOT_STARTED;
    private Map<String, Object> customBranding;
    private PspTestAccountStage currentPspTestAccountStage = PspTestAccountStage.NOT_STARTED;
    private ZonedDateTime createdDate = now(UTC);

    private ServiceDbFixture(DatabaseTestHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public static ServiceDbFixture serviceDbFixture(DatabaseTestHelper databaseTestHelper) {
        return new ServiceDbFixture(databaseTestHelper);
    }

    public ServiceDbFixture withName(String name) {
        this.name = name;
        return this;
    }
    
    public ServiceDbFixture withGatewayAccountIds(String... gatewayAccountIds) {
        this.gatewayAccountIds = Arrays.asList(gatewayAccountIds);
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

    public ServiceDbFixture withGoLiveStage(GoLiveStage goLiveStage) {
        this.goLiveStage = goLiveStage;
        return this;
    }
    
    public ServiceDbFixture withCustomBranding(String cssUrl, String imageUrl) {
        this.customBranding = Map.of(
                "css_url", cssUrl,
                "image_url", imageUrl
        );
        return this;
    }

    public ServiceDbFixture withExperimentalFeaturesEnabled(boolean experimentalFeaturesEnabled) {
        this.experimentalFeaturesEnabled = experimentalFeaturesEnabled;
        return this;
    }

    public ServiceDbFixture withTakesPaymentsOverPhone(boolean takesPaymentsOverPhone) {
        this.takesPaymentsOverPhone = takesPaymentsOverPhone;
        return this;
    }
    
    public ServiceDbFixture withCurrentPspTestAccountStage(PspTestAccountStage currentPspTestAccountStage) {
        this.currentPspTestAccountStage = currentPspTestAccountStage;
        return this;
    }

    public ServiceDbFixture withCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public Service insertService() {
        int serviceId = id == null ? nextInt() : id;
        String extId = externalId == null ? randomUuid() : externalId;

        Service service = Service.from(serviceId, extId, new ServiceName(name));
        service.setMerchantDetails(merchantDetails);
        service.setCollectBillingAddress(collectBillingAddress);
        service.setGoLiveStage(goLiveStage);
        service.setCustomBranding(customBranding);
        service.setExperimentalFeaturesEnabled(experimentalFeaturesEnabled);
        service.setTakesPaymentsOverPhone(takesPaymentsOverPhone);
        service.setCurrentPspTestAccountStage(currentPspTestAccountStage);
        service.setCreatedDate(createdDate);
        databaseHelper.addService(service, gatewayAccountIds.toArray(new String[0]));

        service.setGatewayAccountIds(gatewayAccountIds);
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
