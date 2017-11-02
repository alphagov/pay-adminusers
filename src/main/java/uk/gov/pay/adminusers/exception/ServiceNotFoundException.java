package uk.gov.pay.adminusers.exception;

public class ServiceNotFoundException extends Throwable {

    public ServiceNotFoundException(String serviceExternalId) {
        super("Service with serviceExternalId = \"" + serviceExternalId + "\" NOT FOUND");
    }

}
