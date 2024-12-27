package uk.gov.pay.adminusers.app.config;

import com.google.inject.persist.PersistService;

public class PersistenceServiceInitialiser {

    @jakarta.inject.Inject
    public PersistenceServiceInitialiser(PersistService service) {
        service.start();
    }
}
