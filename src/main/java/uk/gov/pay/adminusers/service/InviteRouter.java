package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

import java.util.Optional;
import java.util.function.Function;

public class InviteRouter {

    private final InviteServiceFactory inviteServiceFactory;
    private final InviteDao inviteDao;

    @Inject
    public InviteRouter(InviteServiceFactory inviteServiceFactory, InviteDao inviteDao) {
        this.inviteServiceFactory = inviteServiceFactory;
        this.inviteDao = inviteDao;
    }

    public Optional<InviteCompleter> routeComplete(String inviteCode) {
        return routeIfExist(inviteCode,
                inviteEntity -> {
                    switch (inviteEntity.getType()) {
                        case SERVICE:
                        case NEW_USER_AND_NEW_SERVICE_SELF_SIGNUP:
                            return inviteServiceFactory.completeSelfSignupInvite();
                        case USER:
                        case EXISTING_USER_INVITED_TO_EXISTING_SERVICE:
                            return inviteServiceFactory.completeExistingUserInvite();
                        case NEW_USER_INVITED_TO_EXISTING_SERVICE:
                            return inviteServiceFactory.completeNewUserExistingServiceInvite();
                        default:
                            throw new IllegalArgumentException(String.format("Unrecognised invite type: %s", inviteEntity.getType()));
                    }
                });
    }
    
    public InviteOtpDispatcher routeOtpDispatch(InviteEntity inviteEntity) {
        InviteType inviteType = inviteEntity.getType();
        switch (inviteType) {
            case SERVICE:
            case NEW_USER_AND_NEW_SERVICE_SELF_SIGNUP:
                return inviteServiceFactory.dispatchServiceOtp();
            case USER:
            case NEW_USER_INVITED_TO_EXISTING_SERVICE:
                return inviteServiceFactory.dispatchUserOtp();
            case EXISTING_USER_INVITED_TO_EXISTING_SERVICE:
                throw new IllegalArgumentException("routeOtpDispatch called on an invite for an existing user");
            default:
                throw new IllegalArgumentException("Unrecognised InviteType: " + inviteType.name());
        }
    }

    private <T> Optional<T> routeIfExist(String inviteCode, Function<InviteEntity, T> routeFunction) {
        return inviteDao.findByCode(inviteCode).map(routeFunction);
    }
}
