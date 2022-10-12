package uk.gov.pay.adminusers.service;

import uk.gov.pay.adminusers.model.InviteCompleteRequest;
import uk.gov.pay.adminusers.model.InviteCompleteResponse;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

public abstract class InviteCompleter {

    /* default */ InviteCompleteRequest data = null;

    public abstract InviteCompleteResponse complete(InviteEntity inviteEntity);

    public InviteCompleter withData(InviteCompleteRequest data) {
        this.data = data;
        return this;
    }
}
