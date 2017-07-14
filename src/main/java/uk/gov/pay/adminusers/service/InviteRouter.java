package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;

import java.util.Optional;

public class InviteRouter {

    private final InviteServiceFactory inviteServiceFactory;
    private final InviteDao inviteDao;

    @Inject
    public InviteRouter(InviteServiceFactory inviteServiceFactory, InviteDao inviteDao) {
        this.inviteServiceFactory = inviteServiceFactory;
        this.inviteDao = inviteDao;
    }

    public Optional<Pair<InviteCompleter, Boolean>> route(String inviteCode) {
        return inviteDao.findByCode(inviteCode)
                .map(inviteEntity -> {
                    boolean isServiceType = InviteType.SERVICE.getType().equals(inviteEntity.getType());
                    InviteCompleter inviteCompleter = isServiceType ? inviteServiceFactory.completeServiceInvite() : inviteServiceFactory.completeUserInvite();
                    return Optional.of(Pair.of(inviteCompleter, isServiceType));
                })
                .orElseGet(() -> Optional.empty());
    }
}
