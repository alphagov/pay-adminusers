package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

public class InviteRouter {

    private final InviteServiceFactory inviteServiceFactory;
    
    @Inject
    public InviteRouter(InviteServiceFactory inviteServiceFactory) {
        this.inviteServiceFactory = inviteServiceFactory;
    }

    public InviteOtpDispatcher routeOtpDispatch(InviteEntity inviteEntity) {
        InviteType inviteType = inviteEntity.getType();
        switch (inviteType) {
            case SERVICE:
                return inviteServiceFactory.dispatchServiceOtp();
            case USER:
                return inviteServiceFactory.dispatchUserOtp();
            default:
                throw new IllegalArgumentException("Unrecognised InviteType: " + inviteType.name());
        }
    }
}
