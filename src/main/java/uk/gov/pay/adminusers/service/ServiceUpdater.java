package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.exception.ServiceNotFoundException;
import uk.gov.pay.adminusers.expungeandarchive.service.ExpungeAndArchiveHistoricalDataService;
import uk.gov.pay.adminusers.model.GoLiveStage;
import uk.gov.pay.adminusers.model.PspTestAccountStage;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceUpdateRequest;
import uk.gov.pay.adminusers.model.UpdateMerchantDetailsRequest;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import static java.util.Map.entry;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingServiceGatewayAccounts;

public class ServiceUpdater {

    public static final String FIELD_GATEWAY_ACCOUNT_IDS = "gateway_account_ids";
    public static final String FIELD_CUSTOM_BRANDING = "custom_branding";
    public static final String FIELD_SERVICE_NAME_PREFIX = "service_name";
    public static final String FIELD_REDIRECT_NAME = "redirect_to_service_immediately_on_terminal_state";
    public static final String FIELD_EXPERIMENTAL_FEATURES_ENABLED = "experimental_features_enabled";
    public static final String FIELD_TAKES_PAYMENTS_OVER_PHONE = "takes_payments_over_phone";
    public static final String FIELD_AGENT_INITIATED_MOTO_ENABLED = "agent_initiated_moto_enabled";
    public static final String FIELD_COLLECT_BILLING_ADDRESS = "collect_billing_address";
    public static final String FIELD_DEFAULT_BILLING_ADDRESS_COUNTRY = "default_billing_address_country";
    public static final String FIELD_CURRENT_GO_LIVE_STAGE = "current_go_live_stage";
    public static final String FIELD_CURRENT_PSP_TEST_ACCOUNT_STAGE = "current_psp_test_account_stage";
    public static final String FIELD_SECTOR = "sector";
    public static final String FIELD_INTERNAL = "internal";
    public static final String FIELD_ARCHIVED = "archived";
    public static final String FIELD_WENT_LIVE_DATE = "went_live_date";
    public static final String FIELD_MERCHANT_DETAILS_NAME = "merchant_details/name";
    public static final String FIELD_MERCHANT_DETAILS_ADDRESS_LINE_1 = "merchant_details/address_line1";
    public static final String FIELD_MERCHANT_DETAILS_ADDRESS_LINE_2 = "merchant_details/address_line2";
    public static final String FIELD_MERCHANT_DETAILS_ADDRESS_CITY = "merchant_details/address_city";
    public static final String FIELD_MERCHANT_DETAILS_ADDRESS_COUNRTY = "merchant_details/address_country";
    public static final String FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE = "merchant_details/address_postcode";
    public static final String FIELD_MERCHANT_DETAILS_EMAIL = "merchant_details/email";
    public static final String FIELD_MERCHANT_DETAILS_TELEPHONE_NUMBER = "merchant_details/telephone_number";
    public static final String FIELD_MERCHANT_DETAILS_URL = "merchant_details/url";
    private final ServiceDao serviceDao;
    private final Map<String, BiConsumer<ServiceUpdateRequest, ServiceEntity>> attributeUpdaters;

    ExpungeAndArchiveHistoricalDataService expungeAndArchiveHistoricalDataService;

    @Inject
    public ServiceUpdater(ServiceDao serviceDao, ExpungeAndArchiveHistoricalDataService expungeAndArchiveHistoricalDataService) {
        Map<String, BiConsumer<ServiceUpdateRequest, ServiceEntity>> attributeUpdaters = new HashMap<>(Map.ofEntries(
                entry(FIELD_GATEWAY_ACCOUNT_IDS, assignGatewayAccounts()),
                entry(FIELD_CUSTOM_BRANDING, updateCustomBranding()),
                entry(FIELD_REDIRECT_NAME, updateRedirectImmediately()),
                entry(FIELD_EXPERIMENTAL_FEATURES_ENABLED, updateExperimentalFeaturesEnabled()),
                entry(FIELD_TAKES_PAYMENTS_OVER_PHONE, updateTakesPaymentsOverPhone()),
                entry(FIELD_AGENT_INITIATED_MOTO_ENABLED, updateAgentInitiatedMotoEnabled()),
                entry(FIELD_COLLECT_BILLING_ADDRESS, updateCollectBillingAddress()),
                entry(FIELD_DEFAULT_BILLING_ADDRESS_COUNTRY, updateDefaultBillingAddressCountry()),
                entry(FIELD_CURRENT_GO_LIVE_STAGE, updateCurrentGoLiveStage()),
                entry(FIELD_CURRENT_PSP_TEST_ACCOUNT_STAGE, updateCurrentPspTestAccountStage()),
                entry(FIELD_SECTOR, updateSector()),
                entry(FIELD_INTERNAL, updateInternal()),
                entry(FIELD_ARCHIVED, updateArchived()),
                entry(FIELD_WENT_LIVE_DATE, updateWentLiveDate()),
                entry(FIELD_MERCHANT_DETAILS_NAME, updateMerchantDetailsName()),
                entry(FIELD_MERCHANT_DETAILS_ADDRESS_LINE_1, updateMerchantDetailsAddressLine1()),
                entry(FIELD_MERCHANT_DETAILS_ADDRESS_LINE_2, updateMerchantDetailsAddressLine2()),
                entry(FIELD_MERCHANT_DETAILS_ADDRESS_CITY, updateMerchantDetailsAddressCity()),
                entry(FIELD_MERCHANT_DETAILS_ADDRESS_COUNRTY, updateMerchantDetailsAddressCountry()),
                entry(FIELD_MERCHANT_DETAILS_ADDRESS_POSTCODE, updateMerchantDetailsAddressPostcode()),
                entry(FIELD_MERCHANT_DETAILS_EMAIL, updateMerchantDetailsEmail()),
                entry(FIELD_MERCHANT_DETAILS_TELEPHONE_NUMBER, updateMerchantDetailsPhone()),
                entry(FIELD_MERCHANT_DETAILS_URL, updateMerchantDetailsUrl())
        ));

        Arrays.stream(SupportedLanguage.values())
                .forEach(language -> attributeUpdaters.put(FIELD_SERVICE_NAME_PREFIX + '/' + language.toString(), updateServiceName()));
        this.attributeUpdaters = Map.copyOf(attributeUpdaters);
        this.serviceDao = serviceDao;
        this.expungeAndArchiveHistoricalDataService = expungeAndArchiveHistoricalDataService;
    }

    @Transactional
    public Optional<Service> doUpdate(String serviceExternalId, ServiceUpdateRequest updateRequests) {
        return doUpdate(serviceExternalId, Collections.singletonList(updateRequests));
    }

    @Transactional
    public Optional<Service> doUpdate(String serviceExternalId, List<ServiceUpdateRequest> updateRequests) {
        return serviceDao.findByExternalId(serviceExternalId)
                .map(serviceEntity -> {
                    updateRequests.forEach(req -> {
                        attributeUpdaters.get(req.getPath())
                                .accept(req, serviceEntity);
                        serviceDao.merge(serviceEntity);
                    });
                    return serviceEntity.toService();
                });
    }

    @Transactional
    public Service doUpdateMerchantDetails(String serviceExternalId, UpdateMerchantDetailsRequest updateMerchantDetailsRequest) throws ServiceNotFoundException {
        return serviceDao.findByExternalId(serviceExternalId)
                .map(serviceEntity -> {
                    MerchantDetailsEntity merchantEntity = MerchantDetailsEntity.from(updateMerchantDetailsRequest);
                    serviceEntity.setMerchantDetailsEntity(merchantEntity);
                    serviceDao.merge(serviceEntity);
                    return serviceEntity.toService();
                }).orElseThrow(() -> new ServiceNotFoundException(serviceExternalId));
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> assignGatewayAccounts() {
        return (serviceUpdateRequest, serviceEntity) -> {
            List<String> gatewayAccountIds = serviceUpdateRequest.valueAsList();
            if (serviceDao.checkIfGatewayAccountsUsed(gatewayAccountIds)) {
                throw conflictingServiceGatewayAccounts(gatewayAccountIds);
            } else {
                serviceEntity.addGatewayAccountIds(gatewayAccountIds.toArray(new String[0]));
            }
        };
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateCustomBranding() {
        return (serviceUpdateRequest, serviceEntity) -> serviceEntity.setCustomBranding(serviceUpdateRequest.valueAsObject());
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateServiceName() {
        return (serviceUpdateRequest, serviceEntity) -> {
            String path = serviceUpdateRequest.getPath();
            assert path.matches(FIELD_SERVICE_NAME_PREFIX + "/[a-z]+") : "Path must be 'service_name/en' etc.";
            SupportedLanguage language = SupportedLanguage.fromIso639AlphaTwoCode(serviceUpdateRequest.getPath().substring(path.indexOf('/') + 1));
            ServiceNameEntity serviceNameEntity = ServiceNameEntity.from(language, serviceUpdateRequest.valueAsString());
            serviceEntity.addOrUpdateServiceName(serviceNameEntity);
        };
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateRedirectImmediately() {
        return (serviceUpdateRequest, serviceEntity) ->
                serviceEntity.setRedirectToServiceImmediatelyOnTerminalState(serviceUpdateRequest.valueAsBoolean());
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateExperimentalFeaturesEnabled() {
        return ((serviceUpdateRequest, serviceEntity) -> serviceEntity.setExperimentalFeaturesEnabled(serviceUpdateRequest.valueAsBoolean()));
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateTakesPaymentsOverPhone() {
        return ((serviceUpdateRequest, serviceEntity) -> serviceEntity.setTakesPaymentsOverPhone(serviceUpdateRequest.valueAsBoolean()));
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateAgentInitiatedMotoEnabled() {
        return ((serviceUpdateRequest, serviceEntity) -> serviceEntity.setAgentInitiatedMotoEnabled(serviceUpdateRequest.valueAsBoolean()));
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateCollectBillingAddress() {
        return (serviceUpdateRequest, serviceEntity) ->
                serviceEntity.setCollectBillingAddress(serviceUpdateRequest.valueAsBoolean());
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateDefaultBillingAddressCountry() {
        return (serviceUpdateRequest, serviceEntity) ->
                serviceEntity.setDefaultBillingAddressCountry(serviceUpdateRequest.valueAsString());
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateCurrentGoLiveStage() {
        return (serviceUpdateRequest, serviceEntity) ->
                serviceEntity.setCurrentGoLiveStage(GoLiveStage.valueOf(serviceUpdateRequest.valueAsString()));
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateCurrentPspTestAccountStage() {
        return (serviceUpdateRequest, serviceEntity) ->
                serviceEntity.setCurrentPspTestAccountStage(PspTestAccountStage.valueOf(serviceUpdateRequest.valueAsString()));
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateSector() {
        return (serviceUpdateRequest, serviceEntity) ->
                serviceEntity.setSector(serviceUpdateRequest.valueAsString());
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateInternal() {
        return ((serviceUpdateRequest, serviceEntity) ->
                serviceEntity.setInternal(serviceUpdateRequest.valueAsBoolean()));
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateArchived() {
        return ((serviceUpdateRequest, serviceEntity) -> {
            if (serviceUpdateRequest.valueAsBoolean()) {
                expungeAndArchiveHistoricalDataService.archiveService(serviceEntity);
            } else {
                serviceEntity.setArchived(serviceUpdateRequest.valueAsBoolean());
                serviceEntity.setArchivedDate(null);
            }
        });
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateWentLiveDate() {
        return ((serviceUpdateRequest, serviceEntity) ->
                serviceEntity.setWentLiveDate(serviceUpdateRequest.valueAsDateTime()));
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateMerchantDetailsName() {
        return (serviceUpdateRequest, serviceEntity) ->
                getOrCreateMerchantDetails(serviceEntity).setName(serviceUpdateRequest.valueAsString());
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateMerchantDetailsAddressLine1() {
        return (serviceUpdateRequest, serviceEntity) ->
                getOrCreateMerchantDetails(serviceEntity).setAddressLine1(serviceUpdateRequest.valueAsString());
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateMerchantDetailsAddressLine2() {
        return (serviceUpdateRequest, serviceEntity) ->
                getOrCreateMerchantDetails(serviceEntity).setAddressLine2(serviceUpdateRequest.valueAsString());
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateMerchantDetailsAddressCity() {
        return (serviceUpdateRequest, serviceEntity) ->
                getOrCreateMerchantDetails(serviceEntity).setAddressCity(serviceUpdateRequest.valueAsString());
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateMerchantDetailsAddressCountry() {
        return (serviceUpdateRequest, serviceEntity) ->
                getOrCreateMerchantDetails(serviceEntity).setAddressCountryCode(serviceUpdateRequest.valueAsString());
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateMerchantDetailsAddressPostcode() {
        return (serviceUpdateRequest, serviceEntity) ->
                getOrCreateMerchantDetails(serviceEntity).setAddressPostcode(serviceUpdateRequest.valueAsString());
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateMerchantDetailsEmail() {
        return (serviceUpdateRequest, serviceEntity) ->
                getOrCreateMerchantDetails(serviceEntity).setEmail(serviceUpdateRequest.valueAsString());
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateMerchantDetailsPhone() {
        return (serviceUpdateRequest, serviceEntity) ->
                getOrCreateMerchantDetails(serviceEntity).setTelephoneNumber(serviceUpdateRequest.valueAsString());
    }

    private BiConsumer<ServiceUpdateRequest, ServiceEntity> updateMerchantDetailsUrl() {
        return (serviceUpdateRequest, serviceEntity) ->
                getOrCreateMerchantDetails(serviceEntity).setUrl(serviceUpdateRequest.valueAsString());
    }

    private MerchantDetailsEntity getOrCreateMerchantDetails(ServiceEntity serviceEntity) {
        if (serviceEntity.getMerchantDetailsEntity() != null) {
            return serviceEntity.getMerchantDetailsEntity();
        }

        MerchantDetailsEntity merchantDetailsEntity = new MerchantDetailsEntity();
        serviceEntity.setMerchantDetailsEntity(merchantDetailsEntity);
        return merchantDetailsEntity;
    }
}
