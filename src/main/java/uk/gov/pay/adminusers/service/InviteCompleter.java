package uk.gov.pay.adminusers.service;

import uk.gov.pay.adminusers.model.InviteCompleteResponse;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

public abstract class InviteCompleter {
    
    public abstract InviteCompleteResponse complete(InviteEntity inviteEntity);
}
