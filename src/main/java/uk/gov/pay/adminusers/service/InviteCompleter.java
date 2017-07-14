package uk.gov.pay.adminusers.service;

import uk.gov.pay.adminusers.model.InviteCompleteRequest;
import uk.gov.pay.adminusers.model.InviteCompleteResponse;

import java.util.Optional;

public abstract class InviteCompleter {

    InviteCompleteRequest data = null;

    public abstract Optional<InviteCompleteResponse> complete(String inviteCode);

    public InviteCompleter withData(InviteCompleteRequest data) {
        this.data = data;
        return this;
    }
}
