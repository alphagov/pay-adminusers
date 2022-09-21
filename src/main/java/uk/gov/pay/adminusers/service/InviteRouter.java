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
                    boolean isServiceType = inviteEntity.isServiceType();
                    InviteCompleter inviteCompleter = isServiceType ? inviteServiceFactory.completeSelfSignupInvite() : inviteServiceFactory.completeExistingUserInvite();
                    return Optional.of(inviteCompleter);
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
