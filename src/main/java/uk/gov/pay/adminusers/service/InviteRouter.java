package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
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

    public Optional<Pair<InviteCompleter, Boolean>> routeComplete(String inviteCode) {
        return routeIfExist(inviteCode,
                inviteEntity -> {
                    boolean isServiceType = InviteType.SERVICE.getType().equals(inviteEntity.getType());
                    InviteCompleter inviteCompleter = isServiceType ? inviteServiceFactory.completeServiceInvite() : inviteServiceFactory.completeUserInvite();
                    return Optional.of(Pair.of(inviteCompleter, isServiceType));
                });
    }

    public Optional<Pair<InviteOtpDispatcher, Boolean>> routeOtpDispatch(String inviteCode) {
        return routeIfExist(inviteCode,
                inviteEntity -> {
                    boolean isUserType = InviteType.USER.getType().equals(inviteEntity.getType());
                    InviteOtpDispatcher inviteOtpDispatcher = isUserType ? inviteServiceFactory.dispatchUserOtp() : inviteServiceFactory.dispatchServiceOtp();
                    return Optional.of(Pair.of(inviteOtpDispatcher, isUserType));
                });

    }

    private <T> Optional<Pair<T, Boolean>> routeIfExist(String inviteCode, Function<InviteEntity, Optional<Pair<T, Boolean>>> routeFunction) {
        return inviteDao.findByCode(inviteCode).map(routeFunction)
                .orElseGet(Optional::empty);
    }
}
