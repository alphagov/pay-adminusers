package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
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
                            return Optional.of(inviteServiceFactory.completeSelfSignupInvite());
                        case USER:
                        case EXISTING_USER_INVITED_TO_EXISTING_SERVICE:
                            return Optional.of(inviteServiceFactory.completeExistingUserInvite());
                        case NEW_USER_INVITED_TO_EXISTING_SERVICE:
                            return Optional.of(inviteServiceFactory.completeNewUserExistingServiceInvite());
                        default:
                            throw new IllegalArgumentException(String.format("Unrecognised invite type: %s", inviteEntity.getType()));
                    }
                });
    }

    public Optional<Pair<InviteOtpDispatcher, Boolean>> routeOtpDispatch(String inviteCode) {
        return routeIfExist(inviteCode,
                inviteEntity -> {
                    boolean isUserType = inviteEntity.isUserType();
                    InviteOtpDispatcher inviteOtpDispatcher = isUserType ? inviteServiceFactory.dispatchUserOtp() : inviteServiceFactory.dispatchServiceOtp();
                    return Optional.of(Pair.of(inviteOtpDispatcher, isUserType));
                });

    }

    private <T> Optional<T> routeIfExist(String inviteCode, Function<InviteEntity, Optional<T>> routeFunction) {
        return inviteDao.findByCode(inviteCode).map(routeFunction)
                .orElseGet(Optional::empty);
    }
}
