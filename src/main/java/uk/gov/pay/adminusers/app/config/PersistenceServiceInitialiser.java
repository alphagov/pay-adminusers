package uk.gov.pay.adminusers.app.config;

import com.google.inject.persist.PersistService;

public class PersistenceServiceInitialiser {

    @javax.inject.Inject
    public PersistenceServiceInitialiser(PersistService service) {
        service.start();
    }
}
