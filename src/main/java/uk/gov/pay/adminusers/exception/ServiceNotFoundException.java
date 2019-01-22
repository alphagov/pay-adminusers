package uk.gov.pay.adminusers.exception;

public class ServiceNotFoundException extends NotFoundException {

    public ServiceNotFoundException(String serviceExternalId) {
        super("Service with serviceExternalId = \"" + serviceExternalId + "\" NOT FOUND");
    }

}
