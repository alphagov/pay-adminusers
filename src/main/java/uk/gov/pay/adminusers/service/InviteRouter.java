package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

import java.util.Optional;

public class InviteRouter {

    private final InviteServiceFactory inviteServiceFactory;
    private final UserServices userService;

    @Inject
    public InviteRouter(InviteServiceFactory inviteServiceFactory, UserServices userService) {
        this.inviteServiceFactory = inviteServiceFactory;
        this.userService = userService;
    }

    public InviteCompleter routeComplete(InviteEntity inviteEntity) {
        switch (inviteEntity.getType()) {
            case SERVICE:
                return inviteServiceFactory.completeSelfSignupInvite();
            case USER: {
                Optional<User> user = userService.findUserByEmail(inviteEntity.getEmail());
                if (user.isPresent()) {
                    return inviteServiceFactory.completeExistingUserInvite();
                } else {
                    return inviteServiceFactory.completeNewUserExistingServiceInvite();
                }
            }
            default:
                throw new IllegalArgumentException(String.format("Unrecognised invite type: %s", inviteEntity.getType()));
        }
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
