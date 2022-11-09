package uk.gov.pay.adminusers.service;

import uk.gov.pay.adminusers.model.CompleteInviteResponse;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

public abstract class InviteCompleter {
    
    public abstract CompleteInviteResponse complete(InviteEntity inviteEntity);
}
