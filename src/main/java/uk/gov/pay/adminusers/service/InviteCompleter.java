package uk.gov.pay.adminusers.service;

import uk.gov.pay.adminusers.model.CompleteInviteResponse;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

import javax.annotation.Nullable;

public abstract class InviteCompleter {
    
    public abstract CompleteInviteResponse complete(InviteEntity inviteEntity, @Nullable SecondFactorMethod secondFactor);
}
